# مستند Monitoring اندروید شکن

این مستند توضیح می‌دهد در اپ اندروید برای monitoring چه کاری انجام می‌شود، چه دیتایی تولید می‌شود، endpointها چطور تنظیم می‌شوند و چه بخش‌هایی هنوز سمت backend/processing نیاز به تصمیم و پیاده‌سازی دارند.

## هدف

هدف monitoring این است که از سمت گوشی کاربر، وضعیت واقعی دسترسی و کیفیت اتصال بررسی شود؛ مثلا کاربر با ISP فعلی به targetهای تعریف‌شده دسترسی دارد یا نه، latency چقدر است، DNS جواب می‌دهد یا نه، proxy node قابل اتصال است یا نه، و یک endpoint HTTP/HTTPS چه response/status/timeای برمی‌گرداند.

نکته مهم این است که این تست‌ها از داخل اپ اندروید و روی شرایط واقعی دستگاه کاربر اجرا می‌شوند، نه از سمت سرور.

## تنظیمات فعلی

اپ آدرس‌های monitoring را از config صفحه home می‌گیرد. نمونه config فعلی:

```json
{
  "monitoring": {
    "target": "https://n8n.coolify.shcn.ir/webhook/api/monitoring/targets",
    "logs": "https://n8n.coolify.shcn.ir/webhook/api/monitoring/logs"
  }
}
```

اگر این config از سرور نیاید یا مقدارها خالی باشند، اپ از مقدارهای پیش‌فرض استفاده می‌کند:

```text
https://my.shecan.ir/monitoring/targets
https://my.shecan.ir/monitoring/logs
```

این config بعد از دریافت home-page در SharedPreferences ذخیره می‌شود و از همان به بعد `MonitoringApi` از همین URLها استفاده می‌کند.

## فلو اجرا

Monitoring داخل `ShecanVpnService` ساخته می‌شود و فقط بعد از فعال شدن VPN شروع می‌شود. یعنی اپ monitoring را به شکل مستقل و دائمی در background اجرا نمی‌کند.

شرایط اجرای تست‌ها:

- VPN شکن باید فعال باشد.
- اپ باید active/foreground باشد.
- دستگاه باید online تشخیص داده شود.
- target list باید از endpoint مربوطه دریافت شده باشد.
- دستگاه باید بر اساس `samplingPercent` داخل sample قرار بگیرد.

وقتی VPN متوقف شود، monitoring هم stop می‌شود و queue داخلی logها پاک می‌شود.

## دریافت targetها

اپ با `GET` به آدرس `monitoring.target` درخواست می‌زند و انتظار دارد پاسخ ساختاری شبیه این داشته باشد:

```json
{
  "version": 1,
  "intervalSeconds": 300,
  "samplingPercent": 100,
  "targets": [
    {
      "id": "de7-dns",
      "type": "dns",
      "ip": "1.2.3.4",
      "domain": "cp.cloudflare.com",
      "timeoutMs": 3000
    },
    {
      "id": "de7-proxy",
      "type": "proxy",
      "ip": "1.2.3.4",
      "port": 443,
      "proxyNode": "de7",
      "datacenter": "de",
      "targetDomain": "de7.check.shecan.ir",
      "timeoutMs": 5000
    },
    {
      "id": "de7-http",
      "type": "http",
      "url": "https://de7.check.shecan.ir",
      "timeoutMs": 5000
    }
  ]
}
```

رفتار targetها:

- `intervalSeconds`: فاصله اجرای تست‌هاست. حداقل داخل اپ ۶۰ ثانیه اعمال می‌شود. اگر مقدار نیاید، پیش‌فرض ۳۰۰ ثانیه است.
- `samplingPercent`: درصد دستگاه‌هایی که monitoring برایشان فعال می‌شود. مقدار بین ۰ تا ۱۰۰ clamp می‌شود. اگر نیاید، پیش‌فرض ۱۰۰ است.
- `targets`: لیست تست‌هایی که اپ باید اجرا کند.

Targetها به مدت ۱۵ دقیقه cache می‌شوند. اگر cache معتبر باشد، اپ همان targetها را دوباره fetch نمی‌کند. اگر دریافت target fail شود یا اینترنت online نباشد، هر ۶۰ ثانیه retry می‌شود.

## نوع تست‌های فعلی

در حال حاضر اپ سه نوع تست را اجرا می‌کند:

### dns

اپ یک DNS query با UDP به IP تعریف‌شده و port 53 می‌فرستد.

ورودی‌های مهم:

- `type`: مقدار `dns`
- `ip`: IP سرور DNS
- `domain`: دامنه‌ای که باید resolve شود. اگر خالی باشد، `cp.cloudflare.com` استفاده می‌شود.
- `timeoutMs`: timeout تست. پیش‌فرض ۳۰۰۰ میلی‌ثانیه.

خروجی‌های مهم:

- `success`: اگر DNS response با rcode صفر باشد true می‌شود.
- `latencyMs`: زمان پاسخ DNS
- `resolvedIp`: اولین A record پیدا شده، اگر قابل استخراج باشد.
- `error`: مقدارهایی مثل `NOERROR`، `SERVFAIL`، `NXDOMAIN`، `REFUSED`
- `errorType`: در صورت exception، نام exception

### proxy

اپ یک TCP connect ساده به IP/port تعریف‌شده انجام می‌دهد. این تست handshake کامل application-level انجام نمی‌دهد؛ هدفش فعلا سنجش reachability و زمان connect به proxy node است.

ورودی‌های مهم:

- `type`: مقدار `proxy`
- `ip`: IP مقصد
- `port`: پورت مقصد. اگر خالی باشد، ۴۴۳ استفاده می‌شود.
- `proxyNode`: نام node، مثلا `de7`
- `datacenter`: دیتاسنتر/کشور
- `targetDomain`: دامنه منطقی target
- `timeoutMs`: timeout تست. پیش‌فرض ۵۰۰۰ میلی‌ثانیه.

خروجی‌های مهم:

- `success`: اگر TCP connect موفق شود true می‌شود.
- `latencyMs`: زمان کل connect
- `tcpConnectMs`: زمان TCP connect
- `errorType` و `error`: در صورت خطا

### http

اپ یک HTTP GET به URL تعریف‌شده می‌زند و status/زمان پاسخ را ثبت می‌کند.

ورودی‌های مهم:

- `type`: مقدار `http`
- `url`: آدرس HTTP/HTTPS مقصد
- `timeoutMs`: timeout تست. پیش‌فرض ۵۰۰۰ میلی‌ثانیه.

خروجی‌های مهم:

- `success`: اگر status بین ۲۰۰ تا ۳۹۹ باشد true می‌شود.
- `httpStatus`: کد status
- `responseTimeMs`: زمان کل پاسخ
- `latencyMs`: فعلا برابر با زمان کل پاسخ
- `tlsSuccess`: اگر URL از جنس HTTPS باشد و request موفق به گرفتن response شود true می‌شود.
- `responseBody`: اگر response JSON باشد، حداکثر ۲۵۶ بایت اول raw body ذخیره می‌شود.

## ارسال logها

بعد از اجرای تست‌ها، اپ logها را به شکل batch با `POST` به آدرس `monitoring.logs` ارسال می‌کند.

ساختار کلی request:

```json
{
  "deviceId": "sha256(android_id)",
  "appVersion": "2.4.0",
  "platform": "android",
  "network": {
    "type": "wifi | mobile | ethernet | vpn | unknown | none",
    "carrier": "MCI",
    "country": "IR"
  },
  "logs": [
    {
      "targetId": "de7-http",
      "eventId": 1,
      "testType": "http",
      "success": true,
      "error": null,
      "errorType": null,
      "userId": "sha256(user_id_or_login)",
      "sessionId": "uuid",
      "isp": "MCI",
      "region": null,
      "domain": null,
      "latencyMs": 180,
      "publicIp": null,
      "resolvedIp": null,
      "proxyNode": null,
      "datacenter": null,
      "targetDomain": null,
      "tcpConnectMs": null,
      "tlsHandshakeMs": null,
      "httpStatus": 200,
      "responseTimeMs": 180,
      "tlsSuccess": true,
      "responseBody": {
        "raw": "{\"ok\":true}"
      },
      "timestamp": "2026-07-18T10:00:00Z",
      "payload": {}
    }
  ]
}
```

نکات privacy/identity:

- `deviceId` به صورت SHA-256 از Android ID ساخته می‌شود.
- `userId` به صورت SHA-256 از user id یا login ذخیره‌شده ساخته می‌شود.
- `sessionId` برای هر instance از `MonitoringIdentity` یک UUID جدید است.
- شماره موبایل/ایمیل/Android ID خام ارسال نمی‌شود.

## Retry و queue

اگر ارسال log موفق نشود، batch داخل queue داخلی نگه داشته می‌شود و هر ۶۰ ثانیه retry می‌شود.

رفتار queue:

- حداکثر ۱۲ batch نگه داشته می‌شود.
- اگر queue پر شود، قدیمی‌ترین batch حذف می‌شود.
- queue فقط در حافظه است و با stop شدن VPN یا بسته شدن process از بین می‌رود.
- logها persistent storage محلی ندارند.

## محدودیت‌های فعلی

این موارد در اپ فعلی وجود دارند، ولی هنوز کامل نیستند یا سمت backend نیاز به کار دارند:

- اپ فقط دیتا را به endpoint می‌فرستد؛ store/process/dashboard سمت backend داخل اپ انجام نمی‌شود.
- `tlsHandshakeMs` در مدل وجود دارد، ولی فعلا جداگانه اندازه‌گیری نمی‌شود.
- DNS shaking/SSL shaking/Cold warm به شکل مستقل با همین نام‌ها پیاده‌سازی نشده‌اند.
- HTTP test فعلی فقط زمان کل response را می‌دهد، نه breakdown کامل page speed/navigation timing.
- proxy test فعلی فقط TCP connect را می‌سنجد و protocol-level یا application-level proxy verification انجام نمی‌دهد.
- queue محلی persistent نیست.
- تغییر نوع تست کاملا جدید بدون update اپ ممکن نیست؛ ولی targetها، timeout، sampling و interval از سمت سرور قابل تغییر هستند.

## جمع‌بندی فنی برای تیم

سمت اندروید الان یک native monitoring collector داریم که:

- از config remote آدرس target/log را می‌گیرد.
- بعد از فعال شدن VPN و در زمان active بودن اپ اجرا می‌شود.
- targetها را از backend می‌گیرد.
- روی گوشی کاربر تست‌های `dns`، `proxy` و `http` را اجرا می‌کند.
- خروجی را با اطلاعات app/network/user/session به endpoint logs ارسال می‌کند.

برای قابل استفاده شدن در dashboard مشترک، بخش‌های زیر باید سمت backend/monitoring platform مشخص شوند:

- schema نهایی و مشترک بین web و android
- endpoint پایدار برای targets
- endpoint پایدار برای logs
- ذخیره‌سازی logها
- پردازش و aggregation
- دسترسی dashboard
- تعریف targetهای مجاز و policy مربوط به sampling/interval/timeout

