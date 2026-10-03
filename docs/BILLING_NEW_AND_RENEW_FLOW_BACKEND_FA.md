# مستند درخواست های اپ برای خرید جدید و تمدید سرویس

تاریخ: ۱۴۰۵/۰۳/۰۶  
نسخه سند: ۰.۱ - مطابق پیاده سازی فعلی اپ اندروید

## هدف سند

این سند مشخص می کند در نسخه فعلی اپ، هنگام انتخاب پلن، اعمال کد تخفیف و پرداخت از طریق سایت، کافه بازار یا مایکت چه اطلاعاتی به بک اند ارسال می شود.

همچنین مواردی که برای تست قطعی خرید جدید و تمدید نیازمند تصمیم بک اند هستند، شفاف شده اند:

1. عنوان پلن در لیست سفارش ها از `/api/services` و تطبیق `peygir_code` دریافت می شود.
2. اپ در وضعیت فعلی بین `خرید جدید` و `تمدید` فیلد متمایزی ارسال نمی کند.

---

## ۱. پلن ها و شناسه های فعلی اپ

### نوع سرویس (`sla`)

| پلن | مقدار ارسالی در API قیمت | شناسه فعلی پلن | عنوان فعلی در اپ |
| --- | --- | ---: | --- |
| برنزی | `bronze` | `47` | برنزی |
| نقره ای | `silver` | `48` | نقره ای |
| طلایی | `gold` | `49` | طلایی |
| تجاری | `commercial` | `83` | تجاری |

> پلن برنزی در UI خرید فعلا قابل خرید نیست، اما شناسه آن در اپ وجود دارد.

### مدت سرویس (`period`)

| مدت | مقدار ارسالی در API قیمت | شناسه مدت |
| --- | --- | ---: |
| یک ماهه | `1m` | `17` |
| سه ماهه | `3m` | `50` |
| شش ماهه | `6m` | `51` |
| سالانه | `1y` | `18` |

### شناسه محصول در مارکت ها (`product_id` / `sku`)

SKU در کافه بازار و مایکت یکسان در نظر گرفته شده است:

```text
shecan_silver_1m
shecan_silver_3m
shecan_silver_6m
shecan_silver_1y

shecan_gold_1m
shecan_gold_3m
shecan_gold_6m
shecan_gold_1y

shecan_commercial_1m
shecan_commercial_3m
shecan_commercial_6m
shecan_commercial_1y
```

---

## ۲. دریافت قیمت قبل از پرداخت

این درخواست در هر سه نسخه `site`، `myket` و `cafebazaar` پس از انتخاب سرویس و مدت زمان ارسال می شود.

```http
POST https://my.shecan.ir/api/price
Content-Type: application/json
X-Redmine-API-Key: USER_API_KEY
```

### نمونه درخواست برای سرویس تجاری یک ماهه

```json
{
  "sla": "commercial",
  "period": "1m",
  "discount": 0
}
```

### مدل پاسخ مورد استفاده اپ

```json
{
  "price": 385000,
  "due_date": "2026-06-27",
  "credit": 0,
  "new_credit": 0,
  "rounded_deference": 0
}
```

اپ در این مرحله از پاسخ، `price` و `due_date` را استفاده می کند.

### محاسبه مبلغ نمایشی و مبلغ پرداخت

مبلغ بازگشتی API ورودی محاسبات اپ است.

برای نسخه سایت:

```text
مبلغ سرویس = round_up_to_10000(ceil(price / 1.1))
مالیات = مبلغ سرویس * 10%
جمع هزینه = مبلغ سرویس + مالیات
```

برای نسخه مایکت و کافه بازار:

```text
مبلغ سرویس = round_up_to_10000(ceil((price / 1.1) * 1.43))
مالیات = مبلغ سرویس * 10%
جمع هزینه = مبلغ سرویس + مالیات
```

مقدار `amount` که در پرداخت یا verify ارسال می شود، `جمع هزینه` محاسبه شده در اپ است.

---

## ۳. اعمال کد تخفیف

در صورت وارد کردن کد تخفیف توسط کاربر، اپ درخواست زیر را ارسال می کند:

```http
POST https://my.shecan.ir/api/discount
Content-Type: application/json
X-Redmine-API-Key: USER_API_KEY
```

### نمونه درخواست

```json
{
  "api_key": "USER_API_KEY",
  "plan_price": 385000,
  "plan_id": 83,
  "phone": "09123456789",
  "code": "DISCOUNT_CODE",
  "duration_id": 17
}
```

| فیلد | توضیح |
| --- | --- |
| `plan_price` | مقدار خام `price` دریافتی از `/api/price` |
| `plan_id` | شناسه پلن، برای تجاری `83` |
| `duration_id` | شناسه مدت، برای یک ماهه `17` |
| `phone` | شماره موبایل نرمال شده کاربر |
| `code` | کد وارد شده توسط کاربر |

اپ از پاسخ موفق، یکی از فیلدهای مبلغ نهایی مانند `finalPrice`، `payable`، `discountedPrice`، `price` یا `planPrice` را به عنوان قیمت پس از تخفیف دریافت می کند.

پس از verify موفق پرداخت درون برنامه ای، اگر تخفیف اعمال شده باشد و `order_id` از verify دریافت شده باشد، درخواست مصرف کد تخفیف ارسال می شود:

```http
POST https://n8n.coolify.shcn.ir/webhook/use-discount
Content-Type: application/json
```

```json
{
  "phone": "09123456789",
  "code": "DISCOUNT_CODE",
  "order_id": 306814,
  "plan_price": 385000
}
```

---

## ۴. جریان پرداخت نسخه سایت

در نسخه سایت، با زدن دکمه پرداخت، اپ درخواست زیر را ارسال می کند:

```http
POST https://my.shecan.ir/order/shecan/payment
Content-Type: application/x-www-form-urlencoded
```

### نمونه body

```text
api_key=USER_API_KEY
amount=385000
sla=commercial
period=1m
discount=0
payload={"sla":"commercial","period":"1m","discount":0}
user={"id":89831,"login":"Old9357706279","api_key":"USER_API_KEY","firstname":"فرشاد","lastname":"اصغرزاده","mail":"user@example.com","created_on":"2025-12-07T08:43:20Z","last_login_on":"2026-05-26T16:23:37Z","admin":false,"status":1,"groups":[],"memberships":[],"welcome_text":"..."}
```

| فیلد | توضیح |
| --- | --- |
| `amount` | جمع هزینه محاسبه شده در اپ |
| `sla` | کلید پلن انتخابی، مثال `commercial` |
| `period` | کلید مدت، مثال `1m` |
| `discount` | میزان تخفیف محاسبه شده، در صورت عدم تخفیف `0` |
| `payload` | خلاصه انتخاب کاربر |
| `user` | اطلاعات حساب کاربر برای سازگاری با جریان فعلی وب |

در صورت موفقیت، پاسخ این endpoint به یک URL درگاه هدایت می کند و اپ همان URL را در مرورگر باز می کند.

---

## ۵. جریان پرداخت مایکت و کافه بازار

در نسخه های مارکت، اپ ابتدا خرید را مستقیما در SDK همان مارکت آغاز می کند:

| نسخه | market در verify | نمونه SKU |
| --- | --- | --- |
| مایکت | `myket` | `shecan_commercial_1m` |
| کافه بازار | `bazaar` | `shecan_commercial_1m` |

پس از خرید موفق یا پس از پیدا شدن خرید قبلی در inventory مارکت، اپ درخواست verify را به بک اند ارسال می کند:

```http
POST https://my.shecan.ir/api/iap/verify
Content-Type: application/json
X-Redmine-API-Key: USER_API_KEY
```

### نمونه درخواست مایکت

```json
{
  "api_key": "USER_API_KEY",
  "market": "myket",
  "package_name": "co.bonyan.shecan",
  "product_id": "shecan_commercial_1m",
  "purchase_token": "PURCHASE_TOKEN_FROM_MYKET",
  "amount": 561000,
  "store_order_id": "MYKET_ORDER_ID"
}
```

### نمونه درخواست کافه بازار

```json
{
  "api_key": "USER_API_KEY",
  "market": "bazaar",
  "package_name": "co.bonyan.shecan",
  "product_id": "shecan_commercial_1m",
  "purchase_token": "PURCHASE_TOKEN_FROM_BAZAAR",
  "amount": 561000,
  "store_order_id": "BAZAAR_ORDER_ID"
}
```

> `amount` نمونه است و براساس قیمت دریافتی و محاسبه نسخه مارکت تولید می شود.

### نکته مهم در پیاده سازی فعلی

اپ در حال حاضر قبل از خرید مارکتی، endpoint ساخت سفارش را صدا نمی زند. بنابراین در درخواست فعلی `/api/iap/verify` فیلد `issue_id` یا `order_id` از سمت اپ ارسال نمی شود.

مدل response مورد استفاده اپ از verify:

```json
{
  "ok": true,
  "duplicate": false,
  "market": "myket",
  "order_id": 306814,
  "payment_id": 1088881,
  "status": "verified"
}
```

در صورت دریافت `ok: true`، اپ پرداخت را موفق در نظر می گیرد. اگر کد تخفیف اعمال شده باشد، از `order_id` پاسخ برای صدا زدن endpoint مصرف تخفیف استفاده می کند.

---

## ۶. دریافت لیست سفارش های کاربر

### دریافت catalog سرویس ها

اپ برای دریافت نام و اطلاعات داینامیک سرویس ها درخواست زیر را ارسال می کند:

```http
GET https://my.shecan.ir/api/services
Referer: site | myket | cafebazaar
```

نمونه پاسخ:

```json
{
  "services": [
    {
      "name_fa": "تجاری",
      "name_en": "Commercial",
      "code": "commercial",
      "peygir_code": 83,
      "purchase_enabled": true,
      "renewal_enabled": true,
      "features": [],
      "allowed_change_codes": []
    }
  ],
  "default_service": "silver",
  "duration": [
    {
      "id": 17,
      "key": "1m",
      "text": "یک ماهه",
      "title": "یک ماهه",
      "bonus": ""
    }
  ]
}
```

اپ پاسخ catalog را ذخیره می کند تا هنگام خطای شبکه آخرین عنوان دریافت شده قابل نمایش باشد.

در بررسی پاسخ واقعی endpoint در تاریخ این سند، headerهای `ETag` و `Cache-Control` در پاسخ `200` وجود نداشتند. در صورت اضافه شدن این headerها از سمت سرور، درخواست `GET` امکان استفاده از validation استاندارد HTTP cache را نیز خواهد داشت.

### دریافت سفارش ها

اپ لیست سرویس ها/سفارش های کاربر را با درخواست زیر دریافت می کند:

```http
GET https://my.shecan.ir/issues.json?offset=0&limit=1000&key=USER_API_KEY
X-Redmine-API-Key: USER_API_KEY
```

در ساختار فعلی، اپ نوع سرویس را از `custom_fields` با ID برابر `58` می خواند و مدت سرویس را از custom field با ID برابر `21`.

### نمونه ساختار فعلی قابل خواندن توسط اپ

```json
{
  "issues": [
    {
      "id": 306814,
      "start_date": "2026-05-27",
      "due_date": "2026-06-27",
      "status": {
        "id": 1,
        "name": "فعال"
      },
      "custom_fields": [
        {
          "id": 58,
          "name": "نوع سرویس",
          "value": "83"
        },
        {
          "id": 21,
          "name": "مدت زمان",
          "value": "17"
        },
        {
          "id": 95,
          "name": "لینک بروزرسان",
          "value": "..."
        }
      ]
    }
  ]
}
```

### نمایش عنوان داینامیک پلن

اپ مقدار custom field شماره `58` سفارش را با `peygir_code` در پاسخ `/api/services` تطبیق می دهد:

```text
issues.custom_fields[id=58].value = 83
services[].peygir_code = 83
services[].name_fa = "تجاری"
```

بنابراین عنوان نمایش داده شده از `services[].name_fa` خوانده می شود و تغییر نام سرویس در آینده نیازمند تغییر نسخه اپ نخواهد بود.

برای عنوان مدت نیز custom field شماره `21` با `duration[].id` تطبیق داده می شود:

```text
issues.custom_fields[id=21].value = 17
duration[].id = 17
duration[].title = "یک ماهه"
```

---

## ۷. وضعیت فعلی خرید جدید و تمدید

### خرید جدید

کاربر وارد صفحه خرید می شود، پلن و مدت را انتخاب می کند، قیمت دریافت می شود و پرداخت شروع می شود.

### تمدید

کاربر از جزئیات یک سرویس موجود روی «تمدید» می زند. اپ فقط همان `sla` و `period` سرویس را به عنوان انتخاب اولیه در صفحه خرید قرار می دهد.

### محدودیت فعلی

در هیچ یک از درخواست های `price`، `payment` یا `iap/verify`، فیلدهای زیر فعلا ارسال نمی شوند:

```json
{
  "action": "new_or_renew",
  "existing_order_id": 306814
}
```

در نتیجه از دید payload فعلی اپ:

- خرید جدید و تمدید، درخواست یکسان دارند.
- اپ مشخص نمی کند پرداخت برای ساخت سرویس جدید است یا تمدید سرویس قبلی.
- اگر بک اند برای تمدید نیازمند شناسه سفارش قبلی است، باید قرارداد API آن اعلام و در اپ اضافه شود.

---

## ۸. موارد مورد نیاز برای تایید بک اند پیش از تست نهایی

لطفا موارد زیر مشخص شوند:

1. برای خرید جدید و تمدید، آیا اپ باید فیلد `action` ارسال کند؟
2. در سناریوی تمدید، آیا اپ باید `existing_order_id` سرویس قبلی را ارسال کند؟
3. این اطلاعات باید در کدام endpoint ارسال شوند:
   - `/api/price`
   - `/order/shecan/payment`
   - `/api/iap/verify`
   - یا یک endpoint جدید برای ساخت سفارش قبل از پرداخت
4. برای IAP مایکت/بازار، آیا `/api/iap/verify` بدون `issue_id` خودش سفارش را ایجاد می کند؟
5. اگر `issue_id` قبل از verify اجباری است، endpoint ساخت سفارش و نمونه request/response آن ارسال شود.

---

## ۹. قرارداد پیشنهادی برای تست خرید جدید و تمدید

اگر تشخیص عملیات بر عهده اپ باشد، حداقل payload مشترک پیشنهادی:

### خرید جدید

```json
{
  "action": "new",
  "sla": "commercial",
  "period": "1m"
}
```

### تمدید

```json
{
  "action": "renew",
  "existing_order_id": 306814,
  "sla": "commercial",
  "period": "1m"
}
```
