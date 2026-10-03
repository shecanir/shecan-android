# پیشنهاد فنی برای تفکیک خرید جدید، تمدید و ارتقا در پرداخت درون برنامه ای

تاریخ: ۱۴۰۵/۰۳/۰۷  
نسخه سند: ۰.۱  
مخاطب: تیم بک اند شکن

## هدف

در پرداخت های کافه بازار و مایکت، مارکت فقط خرید یک `product_id / sku` را تایید می کند و به صورت ذاتی نمی داند این پرداخت برای «خرید جدید»، «تمدید سرویس قبلی» یا «ارتقا» انجام شده است.

بنابراین پیشنهاد می شود قبل از باز کردن صفحه پرداخت مارکت، یک سفارش موقت در بک اند ساخته شود. این سفارش موقت context تجاری پرداخت را نگه می دارد و پس از تایید خرید توسط مارکت، بک اند با استفاده از همان سفارش تصمیم می گیرد سرویس جدید بسازد یا سرویس قبلی را تمدید/ارتقا دهد.

---

## خلاصه راهکار پیشنهادی

1. کاربر در اپ پلن و مدت را انتخاب می کند.
2. اپ قبل از رفتن به کافه بازار/مایکت، یک درخواست `pre-order` به بک اند می زند.
3. بک اند یک `order_id / issue_id` موقت با وضعیت `pending_payment` می سازد.
4. اپ همان `order_id` را داخل `developer payload` مارکت قرار می دهد.
5. کاربر پرداخت را در مارکت انجام می دهد.
6. اپ بعد از برگشت از مارکت، `purchase_token` و `order_id` را برای `/api/iap/verify` ارسال می کند.
7. بک اند خرید را از مارکت verify می کند.
8. اگر verify موفق بود، بک اند بر اساس `action` ذخیره شده در order موقت، عملیات درست را انجام می دهد:
   - `new_purchase`: ساخت سرویس جدید
   - `renewal`: تمدید همان سرویس قبلی
   - `upgrade`: ارتقا یا تغییر پلن سرویس قبلی

---

## چرا pre-order لازم است؟

بدون pre-order، درخواست verify فقط اطلاعات مارکت را دارد:

```json
{
  "market": "myket",
  "product_id": "shecan_silver_1m",
  "purchase_token": "TOKEN_FROM_MARKET",
  "amount": 561000,
  "store_order_id": "MYKET_ORDER_ID"
}
```

این payload مشخص نمی کند پرداخت برای کدام سناریو بوده است. مثلا `shecan_silver_1m` می تواند هر سه حالت زیر باشد:

- خرید اولین سرویس نقره ای
- تمدید سرویس نقره ای موجود
- برگشت از خرید قبلی/restore و تایید دوباره خرید

با pre-order، بک اند قبل از پرداخت می داند کاربر چه قصدی داشته و برای کدام سرویس قبلی پرداخت می کند.

---

## قابلیت مارکت ها برای ارسال context

کافه بازار و مایکت هر دو امکان ارسال `developer payload` هنگام شروع خرید را دارند.

### کافه بازار / Poolakey

```kotlin
val purchaseRequest = PurchaseRequest(
    productId = "shecan_silver_1m",
    payload = "{\"order_id\":1053993,\"action\":\"renewal\"}"
)
```

### مایکت

```java
helper.launchPurchaseFlow(
    activity,
    "shecan_silver_1m",
    purchaseFinishedListener,
    "{\"order_id\":1053993,\"action\":\"renewal\"}"
);
```

نکته مهم: این payload برای کمک به trace و اعتبارسنجی سمت کلاینت مفید است، اما منبع اصلی تصمیم بک اند بهتر است همان `order_id` ساخته شده در pre-order باشد. اپ بعد از خرید نیز `order_id` را صریحا در `/api/iap/verify` ارسال می کند.

---

## Endpoint پیشنهادی ۱: ساخت سفارش موقت قبل از پرداخت

```http
POST /api/iap/pre-order
Content-Type: application/json
X-Redmine-API-Key: USER_API_KEY
```

### فیلدهای پیشنهادی request

| فیلد | اجباری | توضیح |
| --- | --- | --- |
| `market` | بله | `bazaar` یا `myket` |
| `action` | بله | `new_purchase`، `renewal` یا `upgrade` |
| `product_id` | بله | SKU مارکت، مثل `shecan_silver_1m` |
| `sla` | بله | مثل `silver`، `gold`، `commercial` |
| `period` | بله | مثل `1m`، `3m`، `6m`، `1y` |
| `amount` | بله | مبلغ نهایی قابل پرداخت در اپ |
| `discount` | خیر | مبلغ تخفیف، در صورت وجود |
| `discount_code` | خیر | کد تخفیف، در صورت وجود |
| `current_order_id` | فقط تمدید/ارتقا | شناسه سفارش/سرویس فعلی که باید update شود |
| `current_issue_id` | فقط تمدید/ارتقا | اگر شناسه اصلی سمت بک اند issue است |
| `current_service_id` | اختیاری | اگر بک اند شناسه داخلی سرویس جداگانه دارد |
| `from_sla` | فقط ارتقا | پلن فعلی |
| `to_sla` | فقط ارتقا | پلن مقصد |

### نمونه request برای خرید جدید

```json
{
  "market": "bazaar",
  "action": "new_purchase",
  "product_id": "shecan_silver_1m",
  "sla": "silver",
  "period": "1m",
  "amount": 561000,
  "discount": 0
}
```

### نمونه response برای خرید جدید

```json
{
  "ok": true,
  "order_id": 1054000,
  "issue_id": 1054000,
  "status": "pending_payment",
  "payload": {
    "order_id": 1054000,
    "action": "new_purchase"
  }
}
```

### نمونه request برای تمدید

```json
{
  "market": "myket",
  "action": "renewal",
  "current_order_id": 341933,
  "current_issue_id": 341933,
  "product_id": "shecan_silver_1m",
  "sla": "silver",
  "period": "1m",
  "amount": 561000,
  "discount": 0
}
```

### نمونه response برای تمدید

```json
{
  "ok": true,
  "order_id": 1053993,
  "issue_id": 1053993,
  "status": "pending_payment",
  "target_order_id": 341933,
  "payload": {
    "order_id": 1053993,
    "action": "renewal",
    "current_order_id": 341933
  }
}
```

### نمونه request برای ارتقا

```json
{
  "market": "bazaar",
  "action": "upgrade",
  "current_order_id": 341933,
  "current_issue_id": 341933,
  "from_sla": "silver",
  "to_sla": "gold",
  "product_id": "shecan_gold_1m",
  "sla": "gold",
  "period": "1m",
  "amount": 850000,
  "discount": 0
}
```

---

## Endpoint پیشنهادی ۲: تایید خرید بعد از برگشت از مارکت

Endpoint فعلی می تواند توسعه داده شود:

```http
POST /api/iap/verify
Content-Type: application/json
X-Redmine-API-Key: USER_API_KEY
```

### request پیشنهادی

```json
{
  "market": "myket",
  "order_id": 1053993,
  "issue_id": 1053993,
  "package_name": "co.bonyan.shecan",
  "product_id": "shecan_silver_1m",
  "purchase_token": "PURCHASE_TOKEN_FROM_MARKET",
  "amount": 561000,
  "store_order_id": "MARKET_ORDER_ID",
  "developer_payload": {
    "order_id": 1053993,
    "action": "renewal",
    "current_order_id": 341933
  }
}
```

### response موفق برای خرید جدید

```json
{
  "ok": true,
  "duplicate": false,
  "market": "bazaar",
  "order_id": 1054000,
  "payment_id": 1088881,
  "status": "verified",
  "action": "new_purchase",
  "created_order_id": 1054000
}
```

### response موفق برای تمدید

```json
{
  "ok": true,
  "duplicate": false,
  "market": "myket",
  "order_id": 1053993,
  "payment_id": 1088881,
  "status": "verified",
  "action": "renewal",
  "updated_order_id": 341933
}
```

### response خرید تکراری

اگر همان `purchase_token` قبلا verify شده باشد:

```json
{
  "ok": true,
  "duplicate": true,
  "market": "myket",
  "order_id": 1053993,
  "payment_id": 1088881,
  "status": "already_verified",
  "action": "renewal",
  "updated_order_id": 341933
}
```

در حالت duplicate، بک اند نباید دوباره تمدید/ارتقا را اعمال کند. فقط باید نتیجه قبلی را برگرداند.

---

## Flow کامل با مثال تمدید

### ۱. وضعیت اولیه

کاربر یک سرویس فعال دارد:

```json
{
  "order_id": 341933,
  "sla": "silver",
  "period": "1m",
  "due_date": "2026-06-01"
}
```

### ۲. کاربر در اپ تمدید را انتخاب می کند

اپ صفحه خرید را با این context باز می کند:

```json
{
  "action": "renewal",
  "current_order_id": 341933,
  "sla": "silver",
  "period": "1m"
}
```

### ۳. اپ قیمت را دریافت می کند

```http
POST /api/price
```

```json
{
  "sla": "silver",
  "period": "1m",
  "discount": 0
}
```

### ۴. اپ pre-order می سازد

```http
POST /api/iap/pre-order
```

```json
{
  "market": "myket",
  "action": "renewal",
  "current_order_id": 341933,
  "product_id": "shecan_silver_1m",
  "sla": "silver",
  "period": "1m",
  "amount": 561000,
  "discount": 0
}
```

بک اند پاسخ می دهد:

```json
{
  "ok": true,
  "order_id": 1053993,
  "status": "pending_payment",
  "target_order_id": 341933
}
```

### ۵. اپ پرداخت مارکت را باز می کند

اپ این payload را به مایکت/بازار می دهد:

```json
{
  "order_id": 1053993,
  "action": "renewal",
  "current_order_id": 341933
}
```

### ۶. کاربر پرداخت را انجام می دهد

مارکت به اپ برمی گرداند:

```json
{
  "product_id": "shecan_silver_1m",
  "purchase_token": "PURCHASE_TOKEN_FROM_MARKET",
  "store_order_id": "MARKET_ORDER_ID"
}
```

### ۷. اپ verify می زند

```http
POST /api/iap/verify
```

```json
{
  "market": "myket",
  "order_id": 1053993,
  "package_name": "co.bonyan.shecan",
  "product_id": "shecan_silver_1m",
  "purchase_token": "PURCHASE_TOKEN_FROM_MARKET",
  "amount": 561000,
  "store_order_id": "MARKET_ORDER_ID"
}
```

### ۸. بک اند verify و اعمال تمدید را انجام می دهد

بک اند:

1. خرید را از API مایکت/بازار verify می کند.
2. `order_id = 1053993` را پیدا می کند.
3. از order موقت می خواند:

```json
{
  "action": "renewal",
  "target_order_id": 341933
}
```

4. سرویس `341933` را تمدید می کند.
5. سفارش موقت را `verified / paid` می کند.
6. response موفق برمی گرداند.

---

## وضعیت فعلی اپ اندروید

در وضعیت فعلی، اپ:

- خرید مارکت را با `sku` شروع می کند.
- برای بازار و مایکت `developer payload` تولید می کند، اما payload فعلی فقط شامل package، sku، زمان و random id است.
- بعد از خرید، `/api/iap/verify` را صدا می زند.
- در verify فعلی `order_id / issue_id` واقعی برای سناریوی خرید/تمدید ارسال نمی شود.
- بین `new_purchase` و `renewal` و `upgrade` تفکیک قطعی انجام نمی دهد.

بنابراین برای اجرای مدل پیشنهادی، توسعه اپ لازم است.

---

## تغییرات لازم در اپ

1. اضافه شدن مفهوم `BillingAction`:

```text
new_purchase
renewal
upgrade
```

2. هنگام ورود به صفحه خرید از مسیر تمدید، اپ باید شناسه سرویس فعلی را نگه دارد:

```text
current_order_id / current_issue_id
```

3. قبل از باز کردن پرداخت مارکت، اپ باید `/api/iap/pre-order` را صدا بزند.

4. پاسخ pre-order شامل `order_id` باید در state اپ نگهداری شود.

5. `developer payload` مایکت/بازار باید شامل `order_id` و `action` شود.

6. بعد از خرید، اپ باید `order_id` را همراه `purchase_token` به `/api/iap/verify` بفرستد.

7. در صورت restore شدن خرید قبلی از inventory مارکت، اگر pre-order فعال در اپ وجود نداشت، اپ باید بتواند از بک اند کمک بگیرد:
   - یا verify با `purchase_token` انجام شود و بک اند براساس token نتیجه قبلی را برگرداند.
   - یا endpoint جدا برای resolve خرید restore شده تعریف شود.

---

## نکته مهم درباره non-consumable بودن محصولات

در طراحی فعلی گفته شده محصولات در مارکت به صورت `non-consumable` تعریف شوند. اگر کاربر قرار باشد یک SKU یکسان مثل `shecan_silver_1m` را چند بار برای تمدید بخرد، non-consumable ممکن است محدودیت ایجاد کند، چون مارکت می تواند خرید قبلی همان SKU را برگرداند و اجازه خرید مجدد ندهد.

برای سرویس زمان دار و تمدیدهای تکرارشونده، از نظر فنی یکی از این دو تصمیم باید نهایی شود:

1. محصولات بعد از verify موفق، consume شوند تا خرید مجدد همان SKU ممکن باشد.
2. برای هر تمدید، SKU یا مدل محصول به شکلی تعریف شود که مارکت اجازه خرید مجدد بدهد.

اگر قرار است همان SKU در تمدیدهای بعدی دوباره قابل خرید باشد، پیشنهاد فنی اپ این است که پس از verify موفق سمت بک اند، خرید مارکت consume شود. البته تصمیم نهایی باید با سیاست انتشار در کافه بازار/مایکت و منطق بک اند هماهنگ شود.

---

## خطاها و رفتارهای پیشنهادی

### pre-order ناموفق

اگر ساخت سفارش موقت شکست خورد، اپ نباید پرداخت مارکت را باز کند.

```json
{
  "ok": false,
  "error": "invalid_target_order",
  "detail": "current_order_id is not renewable"
}
```

### verify موفق ولی order پیدا نشد

```json
{
  "ok": false,
  "error": "order_not_found",
  "detail": "pre-order not found"
}
```

### verify موفق ولی action نامعتبر

```json
{
  "ok": false,
  "error": "invalid_order_action",
  "detail": "order action does not match product"
}
```

### خرید duplicate

بک اند باید بر اساس `purchase_token` idempotent باشد. اگر token قبلا پردازش شده، نباید دوباره سرویس را تمدید کند.

---

## سوالات مورد نیاز از بک اند

1. نام endpoint پیشنهادی برای pre-order تایید می شود؟
   - پیشنهاد: `POST /api/iap/pre-order`
2. شناسه اصلی برای سرویس فعلی کدام است؟
   - `order_id`
   - `issue_id`
   - یا شناسه داخلی دیگر
3. برای تمدید، بک اند کدام فیلد را لازم دارد؟
   - `current_order_id`
   - `current_issue_id`
   - `current_service_id`
4. برای ارتقا، آیا بک اند سرویس قبلی را update می کند یا سرویس جدید می سازد و قبلی را close می کند؟
5. محصولات مارکت باید بعد از verify مصرف شوند یا non-consumable باقی بمانند؟
6. در حالت restore purchase، اگر اپ pre-order فعال نداشت، بک اند چه response ای برگرداند؟

