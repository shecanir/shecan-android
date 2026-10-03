package ir.shecan.core.billing

object CafeBazaarBillingProducts {

    @JvmStatic
    fun consumableSkus(): List<String> = BillingPlanCatalog.purchasableSkus()

    @JvmStatic
    fun nonConsumableSkus(): List<String> = emptyList()

    @JvmStatic
    fun allSkus(): List<String> = consumableSkus() + nonConsumableSkus()

    @JvmStatic
    fun hasAnySku(): Boolean = allSkus().isNotEmpty()
}
