# جریان فعلی خرید جدید و تمدید در اپ اندروید

تاریخ تطبیق با کد: ۲۰۲۶/۱۰/۱۱

این سند رفتار فعلی `AuthApi`، `BillingPlansFragment` و مدیرهای billing را توضیح می‌دهد. سند `IAP_PRE_ORDER_FLOW_PROPOSAL_FA.md` پیشنهاد پیاده‌سازی آینده است.

## پلن، مدت و قیمت

کلیدهای پلن عبارت‌اند از `silver`، `gold` و `commercial`؛ مدت‌ها `1m`، `3m`، `6m` و `1y` هستند. SKU به شکل `shecan_<sla>_<period>` ساخته می‌شود. عنوان و امکان خرید/تمدید سرویس از catalog دریافت می‌شود:

```http
GET https://my.shecan.ir/api/services
Referer: site | myket | cafebazaar
```

درخواست قیمت:

```http
POST https://my.shecan.ir/api/price
Content-Type: application/json
X-Redmine-API-Key: USER_API_KEY
```

```json
{"sla":"commercial","period":"1m","discount":0}
```

در درخواست قیمت `order_id` ارسال نمی‌شود. در مارکت‌ها، اگر قیمت SKU از SDK در دسترس باشد، همان مبلغ کل مارکت مبنای نمایش و verify است. در مسیر قیمت API، `BillingPlanPrice` مبلغ بدون مالیات را از `price / 1.1` محاسبه می‌کند؛ برای مارکت ضریب `1.43` اعمال می‌شود، مبلغ سرویس به مضرب بعدی ۱۰٬۰۰۰ گرد می‌شود و ۱۰٪ مالیات اضافه می‌شود.

## تخفیف

```http
POST https://my.shecan.ir/api/discount
Content-Type: application/json
```

فیلدهای ارسالی: `api_key`، `plan_price`، `plan_id`، `phone`، `code`، `duration_id`، `market`، `app_version_name` و `app_version_code`.

پس از verify موفق IAP، در صورت وجود کد تخفیف و شناسه سفارش در پاسخ، اپ مصرف تخفیف را ثبت می‌کند:

```http
POST https://n8n.coolify.shcn.ir/webhook/use-discount
Content-Type: application/json
```

```json
{"phone":"09120000000","code":"DISCOUNT_CODE","order_id":306814,"plan_price":385000}
```

## پرداخت نسخه سایت

اپ یک URL می‌سازد و در مرورگر باز می‌کند؛ درخواست form POST با `user` و `payload` از اپ ارسال نمی‌شود.

```text
https://my.shecan.ir/order/shecan/payment?api_key=USER_API_KEY&amount=561000&sla=commercial&period=1m&discount=0&callback_url=CALLBACK_URL
```

`callback_url` از `Constant.AppPaymentCallbackUrl` خوانده می‌شود. اگر تخفیف وارد شده باشد `discount_code` هم اضافه می‌شود. برای تمدید، شناسه سفارش موجود به صورت `order_id` در query اضافه می‌شود:

```text
&order_id=306814
```

در خرید جدید این فیلد وجود ندارد. بعد از برگشت از مرورگر، `SitePaymentCallback` نتیجه را پردازش می‌کند و صفحهٔ اصلی وضعیت سفارش‌ها را تازه می‌کند.

## پرداخت مایکت و کافه بازار

اپ خرید SKU را مستقیماً با SDK آغاز می‌کند و پیش از آن سفارش موقت نمی‌سازد. هر دو مدیر billing برای خرید جدید developer payload زیر را می‌سازند:

```json
{"version":1,"sla":"commercial","period":"1m"}
```

برای تمدید، `order_id` سفارش موجود به همین payload اضافه می‌شود. بعد از خرید موفق یا پیدا شدن خرید قبلی در inventory، درخواست زیر ارسال می‌شود:

```http
POST https://my.shecan.ir/api/iap/verify
Content-Type: application/json
X-Redmine-API-Key: USER_API_KEY
```

```json
{
  "version": 1,
  "api_key": "USER_API_KEY",
  "market": "myket",
  "package_name": "co.bonyan.shecan",
  "product_id": "shecan_commercial_1m",
  "sla": "commercial",
  "period": "1m",
  "purchase_token": "PURCHASE_TOKEN",
  "amount": 561000,
  "store_order_id": "STORE_ORDER_ID",
  "order_id": 306814
}
```

برای بازار `market` برابر `bazaar` است. `order_id` فقط برای تمدید فرستاده می‌شود. `amount` فقط اگر مثبت باشد و `store_order_id` فقط اگر موجود باشد ارسال می‌شوند؛ بنابراین در verify خرید بازیابی‌شده ممکن است مبلغ وجود نداشته باشد. `AuthApi.verifyIap` قابلیت ارسال `issue_id` مثبت را دارد، ولی فراخوانی فعلی `BillingPlansFragment` مقدار صفر می‌دهد و این فیلد را حذف می‌کند.

نمونهٔ پاسخ قابل خواندن توسط اپ:

```json
{"ok":true,"duplicate":false,"market":"myket","order_id":306814,"payment_id":1088881,"status":"verified"}
```

اپ فقط پس از پاسخ موفق verify خرید را تحویل‌شده ثبت می‌کند و مسیر مصرف خرید/تخفیف را ادامه می‌دهد. تأیید اصالت خرید و مبلغ بر عهدهٔ بک‌اند است.

## انتخاب سرویس و تمدید

سفارش‌ها از `issues.json` و عنوان پلن/مدت از catalog خوانده می‌شوند. نوع سرویس از custom field شمارهٔ `58`، مدت از `21` و لینک بروزرسانی از `95` گرفته می‌شود.

هنگام ورود از جزئیات یا لیست سرویس به صفحهٔ تمدید، اپ `sla`، `period` و شناسه همان سفارش را منتقل می‌کند. `order_id` در پرداخت سایت، developer payload مارکت و درخواست verify استفاده می‌شود. فیلدهای `action` و `existing_order_id` در قرارداد فعلی ارسال نمی‌شوند.

این سند قرارداد سمت کلاینت را ثبت می‌کند؛ رفتار سرور برای تمدید و بازیابی خرید باید در محیط تست بک‌اند و حساب‌های آزمایشی مارکت تأیید شود.
