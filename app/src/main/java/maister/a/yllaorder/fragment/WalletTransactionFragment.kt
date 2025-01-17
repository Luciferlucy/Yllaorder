package maister.a.yllaorder.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.flutterwave.raveandroid.RaveConstants
import com.flutterwave.raveandroid.RavePayActivity
import com.flutterwave.raveandroid.RavePayManager
import com.google.gson.Gson
import com.paytm.pgsdk.PaytmOrder
import com.paytm.pgsdk.PaytmPGService
import com.paytm.pgsdk.PaytmPaymentTransactionCallback
import com.razorpay.Checkout
import com.sslcommerz.library.payment.model.datafield.MandatoryFieldModel
import com.sslcommerz.library.payment.model.dataset.TransactionInfo
import com.sslcommerz.library.payment.model.util.CurrencyType
import com.sslcommerz.library.payment.model.util.ErrorKeys
import com.sslcommerz.library.payment.model.util.SdkCategory
import com.sslcommerz.library.payment.model.util.SdkType
import com.sslcommerz.library.payment.viewmodel.listener.OnPaymentResultListener
import com.sslcommerz.library.payment.viewmodel.management.PayUsingSSLCommerz
import org.json.JSONException
import org.json.JSONObject
import maister.a.yllaorder.R
import maister.a.yllaorder.activity.*
import maister.a.yllaorder.adapter.WalletTransactionAdapter
import maister.a.yllaorder.databinding.FragmentWalletTransectionBinding
import maister.a.yllaorder.helper.*
import maister.a.yllaorder.model.Address
import maister.a.yllaorder.model.WalletTransaction
import java.io.Serializable
import kotlin.math.roundToLong

class WalletTransactionFragment : Fragment(), PaytmPaymentTransactionCallback {
    lateinit var binding: FragmentWalletTransectionBinding
    lateinit var root: View
    private lateinit var walletTransactions: ArrayList<WalletTransaction?>
    private lateinit var walletTransactionAdapter: WalletTransactionAdapter
    lateinit var activity: Activity
    lateinit var session: Session
    private lateinit var customerId: String

    var total = 0
    var offset = 0
    var isLoadMore = false

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWalletTransectionBinding.inflate(inflater, container, false)
        root = binding.getRoot();

        activity = requireActivity()
        session = Session(activity)

        val linearLayoutManager = LinearLayoutManager(activity)
        binding.recyclerView.layoutManager = linearLayoutManager

        paymentMethod = ""

        binding.tvAlertTitle.text = getString(R.string.no_wallet_history_found)
        binding.tvAlertSubTitle.text = getString(R.string.you_have_not_any_wallet_history_yet)
        setHasOptionsMenu(true)
        getTransactionData(activity, session)
        binding.swipeLayout.setColorSchemeResources(R.color.colorPrimary)
        binding.swipeLayout.setOnRefreshListener {
            binding.swipeLayout.isRefreshing = false
            offset = 0
            getTransactionData(activity, session)
        }
        ApiConfig.getWalletBalance(activity, session)
        binding.tvBalance.text =
            session.getData(Constant.CURRENCY) + session.getData(Constant.WALLET_BALANCE)
        binding.btnRechargeWallet.setOnClickListener {

            paymentMethod = ""

            val alertDialog = AlertDialog.Builder(requireActivity())
            val inflater1 =
                requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val dialogView = inflater1.inflate(R.layout.dialog_wallet_recharge, null)
            alertDialog.setView(dialogView)
            alertDialog.setCancelable(true)
            val dialog = alertDialog.create()
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val edtAmount: TextView = dialogView.findViewById(R.id.edtAmount)
            val edtMsg: TextView = dialogView.findViewById(R.id.edtMsg)
            val tvDialogCancel: TextView = dialogView.findViewById(R.id.tvDialogCancel)
            val tvDialogSend: TextView = dialogView.findViewById(R.id.tvDialogRecharge)
            val tvWarning: TextView = dialogView.findViewById(R.id.tvWarning)
            val lytPayOption: LinearLayout = dialogView.findViewById(R.id.lytPayOption)
            val rbPayStack: RadioButton = dialogView.findViewById(R.id.rbPayStack)
            val rbFlutterWave: RadioButton = dialogView.findViewById(R.id.rbFlutterWave)
            val rbPayPal: RadioButton = dialogView.findViewById(R.id.rbPayPal)
            val rbRazorPay: RadioButton = dialogView.findViewById(R.id.rbRazorPay)
            val rbMidTrans: RadioButton = dialogView.findViewById(R.id.rbMidTrans)
            val rbStripe: RadioButton = dialogView.findViewById(R.id.rbStripe)
            val rbPayTm: RadioButton = dialogView.findViewById(R.id.rbPayTm)
            val rbSslCommerz: RadioButton = dialogView.findViewById(R.id.rbSslCommerz)
            val lytPayment: RadioGroup = dialogView.findViewById(R.id.lytPayment)
            lytPayment.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
                val rb = dialogView.findViewById<View>(checkedId) as RadioButton
                paymentMethod = rb.tag.toString()
                tvWarning.visibility = View.GONE
            }
            val params: MutableMap<String, String> = HashMap()
            params[Constant.SETTINGS] = Constant.GetVal
            params[Constant.GET_PAYMENT_METHOD] = Constant.GetVal
            ApiConfig.requestToVolley(object : VolleyCallback {
                override fun onSuccess(result: Boolean, response: String) {
                    if (result) {
                        try {
                            val jsonObject1 = JSONObject(response)
                            if (!jsonObject1.getBoolean(Constant.ERROR)) {
                                if (jsonObject1.has(Constant.PAYMENT_METHODS)) {
                                    val jsonObject =
                                        jsonObject1.getJSONObject(Constant.PAYMENT_METHODS)
                                    if (jsonObject.has(Constant.cod_payment_method)) {
                                        Constant.COD =
                                            jsonObject.getString(Constant.cod_payment_method)
                                        Constant.COD_MODE = jsonObject.getString(Constant.cod_mode)
                                    }
                                    if (jsonObject.has(Constant.razor_pay_method)) {
                                        Constant.RAZORPAY =
                                            jsonObject.getString(Constant.razor_pay_method)
                                        Constant.RAZOR_PAY_KEY_VALUE =
                                            jsonObject.getString(Constant.RAZOR_PAY_KEY)
                                    }
                                    if (jsonObject.has(Constant.paypal_method)) {
                                        Constant.PAYPAL =
                                            jsonObject.getString(Constant.paypal_method)
                                    }
                                    if (jsonObject.has(Constant.paystack_method)) {
                                        Constant.PAYSTACK =
                                            jsonObject.getString(Constant.paystack_method)
                                        Constant.PAYSTACK_KEY =
                                            jsonObject.getString(Constant.paystack_public_key)
                                    }
                                    if (jsonObject.has(Constant.flutterwave_payment_method)) {
                                        Constant.FLUTTERWAVE =
                                            jsonObject.getString(Constant.flutterwave_payment_method)
                                        Constant.FLUTTERWAVE_ENCRYPTION_KEY_VAL =
                                            jsonObject.getString(Constant.flutterwave_encryption_key)
                                        Constant.FLUTTERWAVE_PUBLIC_KEY_VAL =
                                            jsonObject.getString(Constant.flutterwave_public_key)
                                        Constant.FLUTTERWAVE_SECRET_KEY_VAL =
                                            jsonObject.getString(Constant.flutterwave_secret_key)
                                        Constant.FLUTTERWAVE_SECRET_KEY_VAL =
                                            jsonObject.getString(Constant.flutterwave_secret_key)
                                        Constant.FLUTTERWAVE_CURRENCY_CODE_VAL =
                                            jsonObject.getString(Constant.flutterwave_currency_code)
                                    }
                                    if (jsonObject.has(Constant.midtrans_payment_method)) {
                                        Constant.MIDTRANS =
                                            jsonObject.getString(Constant.midtrans_payment_method)
                                    }
                                    if (jsonObject.has(Constant.stripe_payment_method)) {
                                        Constant.STRIPE =
                                            jsonObject.getString(Constant.stripe_payment_method)
                                        isAddressAvailable()
                                    }
                                    if (jsonObject.has(Constant.paytm_payment_method)) {
                                        Constant.PAYTM =
                                            jsonObject.getString(Constant.paytm_payment_method)
                                        Constant.PAYTM_MERCHANT_ID =
                                            jsonObject.getString(Constant.paytm_merchant_id)
                                        Constant.PAYTM_MERCHANT_KEY =
                                            jsonObject.getString(Constant.paytm_merchant_key)
                                        Constant.PAYTM_MODE =
                                            jsonObject.getString(Constant.paytm_mode)
                                    }
                                    if (jsonObject.has(Constant.ssl_commerce_payment_method)) {
                                        Constant.SSLECOMMERZ =
                                            jsonObject.getString(Constant.ssl_commerce_payment_method)
                                        Constant.SSLECOMMERZ_MODE =
                                            jsonObject.getString(Constant.ssl_commerece_mode)
                                        Constant.SSLECOMMERZ_STORE_ID =
                                            jsonObject.getString(Constant.ssl_commerece_store_id)
                                        Constant.SSLECOMMERZ_SECRET_KEY =
                                            jsonObject.getString(Constant.ssl_commerece_secret_key)
                                    }
                                    if (jsonObject.has(Constant.direct_bank_transfer_method)) {
                                        Constant.DIRECT_BANK_TRANSFER =
                                            jsonObject.getString(Constant.direct_bank_transfer_method)
                                        Constant.ACCOUNT_NAME =
                                            jsonObject.getString(Constant.account_name)
                                        Constant.ACCOUNT_NUMBER =
                                            jsonObject.getString(Constant.account_number)
                                        Constant.BANK_NAME =
                                            jsonObject.getString(Constant.bank_name)
                                        Constant.BANK_CODE =
                                            jsonObject.getString(Constant.bank_code)
                                        Constant.NOTES = jsonObject.getString(Constant.notes)
                                    }

                                    if (Constant.FLUTTERWAVE == "0" && Constant.PAYPAL == "0" && Constant.COD == "0" && Constant.RAZORPAY == "0" && Constant.PAYSTACK == "0" && Constant.MIDTRANS == "0" && Constant.STRIPE == "0" && Constant.PAYTM == "0" && Constant.SSLECOMMERZ == "0") {
                                        lytPayOption.visibility = View.GONE
                                    } else {
                                        lytPayOption.visibility = View.VISIBLE
                                        if (Constant.RAZORPAY == "1") {
                                            rbRazorPay.visibility = View.VISIBLE
                                        }
                                        if (Constant.PAYSTACK == "1") {
                                            rbPayStack.visibility = View.VISIBLE
                                        }
                                        if (Constant.FLUTTERWAVE == "1") {
                                            rbFlutterWave.visibility = View.VISIBLE
                                        }
                                        if (Constant.PAYPAL == "1") {
                                            rbPayPal.visibility = View.VISIBLE
                                        }
                                        if (Constant.MIDTRANS == "1") {
                                            rbMidTrans.visibility = View.VISIBLE
                                        }
                                        if (Constant.STRIPE == "1") {
                                            rbStripe.visibility = View.VISIBLE
                                        }
                                        if (Constant.PAYTM == "1") {
                                            rbPayTm.visibility = View.VISIBLE
                                        }
                                        if (Constant.SSLECOMMERZ == "1") {
                                            rbSslCommerz.visibility = View.VISIBLE
                                        }
                                    }
                                } else {
                                    Toast.makeText(
                                        activity,
                                        getString(R.string.alert_payment_methods_blank),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                }
            }, activity, Constant.SETTING_URL, params, false)
            tvDialogSend.setOnClickListener {
                if (edtAmount.text.toString() == "") {
                    edtAmount.requestFocus()
                    edtAmount.error = getString(R.string.alert_enter_amount)
                } else if (edtAmount.text.toString()
                        .toDouble() > session.getData(Constant.user_wallet_refill_limit)!!
                        .toDouble()
                ) {
                    Toast.makeText(
                        activity,
                        getString(R.string.max_wallet_amt_error),
                        Toast.LENGTH_SHORT
                    ).show()
                } else if (edtAmount.text.toString().trim().toDouble() <= 0) {
                    edtAmount.requestFocus()
                    edtAmount.error = getString(R.string.alert_recharge)
                } else {
                    amount = edtAmount.text.toString().trim()
                    msg = edtMsg.text.toString().trim()
                    rechargeWallet(activity, dialog, tvWarning)
                }
            }
            tvDialogCancel.setOnClickListener { dialog.dismiss() }
            dialog.show()
        }
        return binding.root
    }

    private fun isAddressAvailable() {
        val params: MutableMap<String, String> = HashMap()
        params[Constant.GET_ADDRESSES] = Constant.GetVal
        params[Constant.USER_ID] = session.getData(Constant.ID).toString()
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val jsonObject = JSONObject(response)
                        if (!jsonObject.getBoolean(Constant.ERROR)) {
                            total = jsonObject.getString(Constant.TOTAL).toInt()
                            session.setData(Constant.TOTAL, total.toString())
                            val `object` = JSONObject(response)
                            val jsonArray = `object`.getJSONArray(Constant.DATA)

                            for (i in 0 until jsonArray.length()) {
                                val jsonObject1 = jsonArray.getJSONObject(i)
                                if (jsonObject1 != null) {
                                    val address =
                                        Gson().fromJson(
                                            jsonObject1.toString(),
                                            Address::class.java
                                        )
                                    if (address.is_default == "1") {
                                        Constant.DefaultAddress =
                                            address.address + ", " + address.landmark + ", " + address.city + ", " + address.area + ", " + address.state + ", " + address.country + ", " + activity.getString(
                                                R.string.pincode_
                                            ) + address.pincode
                                        Constant.DefaultCity = address.city
                                        Constant.DefaultPinCode = address.pincode
                                    }
                                }
                            }
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
        }, activity, Constant.GET_ADDRESS_URL, params, false)
    }

    @SuppressLint("SetTextI18n")
    fun addWalletBalance(activity: Activity, session: Session, amount: String?, msg: String?) {
        val params: MutableMap<String, String> = HashMap()
        params[Constant.ADD_WALLET_BALANCE] = Constant.GetVal
        params[Constant.USER_ID] = session.getData(Constant.ID).toString()
        params[Constant.AMOUNT] = amount.toString()
        params[Constant.TYPE] = Constant.CREDIT
        params[Constant.MESSAGE] = msg.toString()
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val `object` = JSONObject(response)
                        if (!`object`.getBoolean(Constant.ERROR)) {
                            session.setData(
                                Constant.WALLET_BALANCE,
                                `object`.getString(Constant.NEW_BALANCE)
                            )
                            binding.tvBalance.text =
                                session.getData(Constant.CURRENCY) + `object`.getString(
                                    Constant.NEW_BALANCE
                                )
                            DrawerFragment.tvWallet1.text =
                                session.getData(Constant.CURRENCY) + `object`.getString(
                                    Constant.NEW_BALANCE
                                )
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
        }, activity, Constant.TRANSACTION_URL, params, true)
    }

    private fun startPaypalPayment(sendParams: MutableMap<String, String>) {
        val params: MutableMap<String, String> = HashMap()
        params[Constant.FIRST_NAME] = session.getData(Constant.NAME).toString()
        params[Constant.LAST_NAME] = session.getData(Constant.NAME).toString()
        params[Constant.PAYER_EMAIL] = session.getData(Constant.EMAIL).toString()
        params[Constant.ITEM_NAME] = getString(R.string.wallet_recharge)
        params[Constant.ITEM_NUMBER] =
            "wallet-refill-user-" + Session(activity).getData(Constant.ID) + "-" + System.currentTimeMillis()
        params[Constant.AMOUNT] = sendParams[Constant.FINAL_TOTAL].toString()
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    val intent = Intent(activity, PayPalWebActivity::class.java)
                    intent.putExtra(Constant.URL, response)
                    intent.putExtra(
                        Constant.ORDER_ID, "wallet-refill-user-" + Session(activity).getData(
                            Constant.ID
                        ) + "-" + System.currentTimeMillis()
                    )
                    intent.putExtra(Constant.FROM, Constant.WALLET)
                    intent.putExtra(Constant.PARAMS, sendParams as Serializable)
                    startActivity(intent)
                }
            }
        }, activity, Constant.PAPAL_URL, params, true)
    }

    private fun callPayStack(sendParams: MutableMap<String, String>) {
        val intent = Intent(activity, PayStackActivity::class.java)
        intent.putExtra(Constant.PARAMS, sendParams as Serializable?)
        startActivity(intent)
    }

    private fun startFlutterWavePayment() {
        RavePayManager(this)
            .setAmount(amount.toDouble())
            .setEmail(session.getData(Constant.EMAIL))
            .setCurrency(Constant.FLUTTERWAVE_CURRENCY_CODE_VAL)
            .setfName(session.getData(Constant.FIRST_NAME))
            .setlName(session.getData(Constant.LAST_NAME))
            .setNarration(getString(R.string.app_name) + getString(R.string.shopping))
            .setPublicKey(Constant.FLUTTERWAVE_PUBLIC_KEY_VAL)
            .setEncryptionKey(Constant.FLUTTERWAVE_ENCRYPTION_KEY_VAL)
            .setTxRef(System.currentTimeMillis().toString() + "Ref")
            .acceptAccountPayments(true)
            .acceptCardPayments(true)
            .acceptAccountPayments(true)
            .acceptAchPayments(true)
            .acceptBankTransferPayments(true)
            .acceptBarterPayments(true)
            .acceptGHMobileMoneyPayments(true)
            .acceptRwfMobileMoneyPayments(true)
            .acceptSaBankPayments(true)
            .acceptFrancMobileMoneyPayments(true)
            .acceptZmMobileMoneyPayments(true)
            .acceptUssdPayments(true)
            .acceptUkPayments(true)
            .acceptMpesaPayments(true)
            .shouldDisplayFee(true)
            .onStagingEnv(false)
            .showStagingLabel(false)
            .initialize()
    }

    private fun startSslCommerzPayment(
        activity: Activity,
        amount: String?,
        msg: String?,
        txnId: String?
    ) {
        val mode: String = if (Constant.SSLECOMMERZ_MODE == "sandbox") {
            SdkType.TESTBOX
        } else {
            SdkType.LIVE
        }
        val mandatoryFieldModel = MandatoryFieldModel(
            Constant.SSLECOMMERZ_STORE_ID,
            Constant.SSLECOMMERZ_SECRET_KEY,
            amount,
            txnId,
            CurrencyType.BDT,
            mode,
            SdkCategory.BANK_LIST
        )

        /* Call for the payment */PayUsingSSLCommerz.getInstance()
            .setData(activity, mandatoryFieldModel, object : OnPaymentResultListener {
                override fun transactionSuccess(transactionInfo: TransactionInfo) {
                    // If payment is success and risk label is 0.
                    addWalletBalance(activity, Session(activity), amount, msg)
                }

                override fun transactionFail(sessionKey: String) {
                    Toast.makeText(
                        activity,
                        "transactionFail -> Session : $sessionKey",
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun error(errorCode: Int) {
                    when (errorCode) {
                        ErrorKeys.USER_INPUT_ERROR -> Toast.makeText(
                            activity,
                            "User Input Error",
                            Toast.LENGTH_LONG
                        ).show()
                        ErrorKeys.INTERNET_CONNECTION_ERROR -> Toast.makeText(
                            activity,
                            "Internet Connection Error",
                            Toast.LENGTH_LONG
                        ).show()
                        ErrorKeys.DATA_PARSING_ERROR -> Toast.makeText(
                            activity,
                            "Data Parsing Error",
                            Toast.LENGTH_LONG
                        ).show()
                        ErrorKeys.CANCEL_TRANSACTION_ERROR -> Toast.makeText(
                            activity,
                            "User Cancel The Transaction",
                            Toast.LENGTH_LONG
                        ).show()
                        ErrorKeys.SERVER_ERROR -> Toast.makeText(
                            activity,
                            "Server Error",
                            Toast.LENGTH_LONG
                        ).show()
                        ErrorKeys.NETWORK_ERROR -> Toast.makeText(
                            activity,
                            "Network Error",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            })
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
         if (requestCode == RaveConstants.RAVE_REQUEST_CODE && data != null) {
            when (resultCode) {
                RavePayActivity.RESULT_SUCCESS -> {
                    addWalletBalance(activity, Session(activity), amount, msg)
                    Toast.makeText(
                        activity,
                        getString(R.string.wallet_recharged),
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
                RavePayActivity.RESULT_ERROR -> {
                    Toast.makeText(
                        activity,
                        getString(R.string.order_error),
                        Toast.LENGTH_LONG
                    ).show()
                }
                RavePayActivity.RESULT_CANCELLED -> {
                    Toast.makeText(
                        activity,
                        getString(R.string.order_cancel),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun startPayTmPayment() {
        val params: MutableMap<String, String> = HashMap()
        params[Constant.ORDER_ID_] = Constant.randomAlphaNumeric(20)
        params[Constant.CUST_ID] = Constant.randomAlphaNumeric(10)
        params[Constant.TXN_AMOUNT] = "" + amount
        if (Constant.PAYTM_MODE == "sandbox") {
            params[Constant.INDUSTRY_TYPE_ID] = Constant.INDUSTRY_TYPE_ID_DEMO_VAL
            params[Constant.CHANNEL_ID] = Constant.MOBILE_APP_CHANNEL_ID_DEMO_VAL
            params[Constant.WEBSITE] = Constant.WEBSITE_DEMO_VAL
        } else if (Constant.PAYTM_MODE == "production") {
            params[Constant.INDUSTRY_TYPE_ID] = Constant.INDUSTRY_TYPE_ID_LIVE_VAL
            params[Constant.CHANNEL_ID] = Constant.MOBILE_APP_CHANNEL_ID_LIVE_VAL
            params[Constant.WEBSITE] = Constant.WEBSITE_LIVE_VAL
        }

        ApiConfig.requestToVolley(object : VolleyCallback, PaytmPaymentTransactionCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val jsonObject = JSONObject(response)
                        val `object` = jsonObject.getJSONObject(Constant.DATA)
                        val service = if (Constant.PAYTM_MODE == "sandbox") {
                            PaytmPGService.getStagingService(Constant.PAYTM_ORDER_PROCESS_DEMO_VAL)
                        } else {
                            PaytmPGService.getProductionService()
                        }
                        customerId = `object`.getString(Constant.CUST_ID)
                        //creating a hashmap and adding all the values required
                        val paramMap = HashMap<String, String>()
                        paramMap[Constant.MID] = Constant.PAYTM_MERCHANT_ID
                        paramMap[Constant.ORDER_ID_] = `object`.getString(Constant.ORDER_ID_)
                        paramMap[Constant.CUST_ID] =
                            `object`.getString(Constant.CUST_ID)
                        paramMap[Constant.TXN_AMOUNT] = "" + amount
                        if (Constant.PAYTM_MODE == "sandbox") {
                            paramMap[Constant.INDUSTRY_TYPE_ID] =
                                Constant.INDUSTRY_TYPE_ID_LIVE_VAL
                            paramMap[Constant.CHANNEL_ID] =
                                Constant.MOBILE_APP_CHANNEL_ID_DEMO_VAL
                            paramMap[Constant.WEBSITE] = Constant.WEBSITE_DEMO_VAL
                        } else if (Constant.PAYTM_MODE == "production") {
                            paramMap[Constant.INDUSTRY_TYPE_ID] =
                                Constant.INDUSTRY_TYPE_ID_DEMO_VAL
                            paramMap[Constant.CHANNEL_ID] =
                                Constant.MOBILE_APP_CHANNEL_ID_LIVE_VAL
                            paramMap[Constant.WEBSITE] = Constant.WEBSITE_LIVE_VAL
                        }
                        paramMap[Constant.CALLBACK_URL] =
                            `object`.getString(Constant.CALLBACK_URL)
                        paramMap[Constant.CHECKSUMHASH] =
                            jsonObject.getString("signature")

                        //creating a paytm order object using the hashmap
                        val order = PaytmOrder(paramMap)

                        //intializing the paytm service
                        service.initialize(order, null)

                        //finally starting the payment transaction
                        service!!.startPaymentTransaction(activity, true, true, this)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }

            }

            override fun onTransactionResponse(bundle: Bundle) {
                val orderId = bundle.getString(Constant.ORDERID)
                val status = bundle.getString(Constant.STATUS_)
                if (status.equals(Constant.TXN_SUCCESS, ignoreCase = true)) {
                    verifyTransaction(orderId)
                }
            }

            override fun networkNotAvailable() {}
            override fun clientAuthenticationFailed(inErrorMessage: String) {}
            override fun someUIErrorOccurred(inErrorMessage: String) {}
            override fun onErrorLoadingWebPage(
                iniErrorCode: Int,
                inErrorMessage: String,
                inFailingUrl: String
            ) {
            }

            override fun onBackPressedCancelTransaction() {}
            override fun onTransactionCancel(
                inErrorMessage: String,
                inResponse: Bundle
            ) {
            }
        }, activity, Constant.GENERATE_PAYTM_CHECKSUM, params, false)
    }

    override fun onTransactionResponse(bundle: Bundle) {
        val orderId = bundle.getString(Constant.ORDERID)
        val status = bundle.getString(Constant.STATUS_)
        if (status.equals(Constant.TXN_SUCCESS, ignoreCase = true)) {
            verifyTransaction(orderId)
        }
    }

    /**
     * Verifying the transaction status once PayTM transaction is over
     * This makes server(own) -> server(PayTM) call to verify the transaction status
     */
    private fun verifyTransaction(orderId: String?) {
        val params: MutableMap<String, String> = HashMap()
        params["orderId"] = orderId.toString()
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val jsonObject = JSONObject(response)
                        val status = jsonObject.getJSONObject("body")
                            .getJSONObject("resultInfo")
                            .getString("resultStatus")
                        if (status.equals("TXN_SUCCESS", ignoreCase = true)) {
                            addWalletBalance(
                                activity,
                                Session(activity),
                                amount,
                                msg
                            )
                            Toast.makeText(
                                activity,
                                getString(R.string.wallet_recharged),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
        }, activity, Constant.VALID_TRANSACTION, params, false)
    }

    override fun networkNotAvailable() {}
    override fun clientAuthenticationFailed(inErrorMessage: String) {}
    override fun someUIErrorOccurred(inErrorMessage: String) {}
    override fun onErrorLoadingWebPage(
        iniErrorCode: Int,
        inErrorMessage: String,
        inFailingUrl: String
    ) {
    }

    override fun onBackPressedCancelTransaction() {}
    override fun onTransactionCancel(
        inErrorMessage: String,
        inResponse: Bundle
    ) {
    }

    private fun createOrderId(payable: Double) {
        val params: MutableMap<String, String> = HashMap()
        params["amount"] = "" + payable.roundToLong() + "00"
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val `object` = JSONObject(response)
                        if (!`object`.getBoolean(Constant.ERROR)) {
                            startPayment(
                                `object`.getString(Constant.ID),
                                `object`.getString("amount")
                            )
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
        }, activity, Constant.GET_RAZORPAY_ORDER_URL, params, true)
    }

    private fun startPayment(orderId: String?, payAmount: String?) {
        val checkout = Checkout()
        checkout.setKeyID(Constant.RAZOR_PAY_KEY_VALUE)
        checkout.setImage(R.mipmap.ic_launcher)
        try {
            val options = JSONObject()
            options.put(Constant.NAME, session.getData(Constant.NAME))
            options.put(Constant.ORDER_ID, orderId)
            options.put(Constant.CURRENCY, "INR")
            options.put(Constant.AMOUNT, payAmount)
            val preFill = JSONObject()
            preFill.put(Constant.EMAIL, session.getData(Constant.EMAIL))
            preFill.put(Constant.CONTACT, session.getData(Constant.MOBILE))
            options.put("prefill", preFill)
            checkout.open(requireActivity(), options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getTransactionData(
        activity: Activity,
        session: Session
    ) {
        binding.recyclerView.visibility = View.GONE
        binding.shimmerFrameLayout.visibility = View.VISIBLE
        binding.shimmerFrameLayout.startShimmer()
        walletTransactions = ArrayList()
        val params: MutableMap<String, String> = HashMap()
        params[Constant.GET_USER_TRANSACTION] = Constant.GetVal
        params[Constant.USER_ID] = session.getData(Constant.ID).toString()
        params[Constant.TYPE] = Constant.TYPE_WALLET_TRANSACTION
        params[Constant.OFFSET] = "" + offset
        params[Constant.LIMIT] = "" + Constant.LOAD_ITEM_LIMIT
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val jsonObject = JSONObject(response)
                        if (!jsonObject.getBoolean(Constant.ERROR)) {
                            total = jsonObject.getString(Constant.TOTAL)
                                .toInt()
                            session.setData(
                                Constant.TOTAL,
                                total.toString()
                            )
                            val `object` =
                                JSONObject(response)
                            val jsonArray =
                                `object`.getJSONArray(Constant.DATA)

                            for (i in 0 until jsonArray.length()) {
                                val jsonObject1 =
                                    jsonArray.getJSONObject(i)
                                if (jsonObject1 != null) {
                                    val transaction = Gson().fromJson(
                                        jsonObject1.toString(),
                                        WalletTransaction::class.java
                                    )
                                    walletTransactions.add(transaction)
                                } else {
                                    break
                                }
                            }
                            if (offset == 0) {
                                walletTransactionAdapter =
                                    WalletTransactionAdapter(
                                        activity,
                                        walletTransactions
                                    )
                                walletTransactionAdapter.setHasStableIds(
                                    true
                                )
                                binding.recyclerView.adapter =
                                    walletTransactionAdapter
                                binding.shimmerFrameLayout.stopShimmer()
                                binding.shimmerFrameLayout.visibility =
                                    View.GONE
                                binding.recyclerView.visibility = View.VISIBLE
                                binding.scrollView.setOnScrollChangeListener { v: NestedScrollView, _: Int, scrollY: Int, _: Int, _: Int ->

                                    // if (diff == 0) {
                                    if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                                        val linearLayoutManager =
                                            binding.recyclerView.layoutManager as LinearLayoutManager?
                                        if (walletTransactions.size < total) {
                                            if (!isLoadMore) {
                                                if (linearLayoutManager != null && linearLayoutManager.findLastCompletelyVisibleItemPosition() == walletTransactions.size - 1) {
                                                    //bottom of list!

                                                    offset += Constant.LOAD_ITEM_LIMIT
                                                    val params1: MutableMap<String, String> =
                                                        HashMap()
                                                    params1[Constant.GET_USER_TRANSACTION] =
                                                        Constant.GetVal
                                                    params1[Constant.USER_ID] =
                                                        session.getData(
                                                            Constant.ID
                                                        ).toString()
                                                    params1[Constant.TYPE] =
                                                        Constant.TYPE_WALLET_TRANSACTION
                                                    params1[Constant.OFFSET] =
                                                        "" + offset
                                                    params1[Constant.LIMIT] =
                                                        "" + Constant.LOAD_ITEM_LIMIT
                                                    ApiConfig.requestToVolley(
                                                        object :
                                                            VolleyCallback {
                                                            override fun onSuccess(
                                                                result: Boolean,
                                                                response: String
                                                            ) {
                                                                if (result) {
                                                                    try {

                                                                        val jsonObject2 =
                                                                            JSONObject(
                                                                                response
                                                                            )
                                                                        if (!jsonObject.getBoolean(
                                                                                Constant.ERROR
                                                                            )
                                                                        ) {
                                                                            session.setData(
                                                                                Constant.TOTAL,
                                                                                jsonObject2.getString(
                                                                                    Constant.TOTAL
                                                                                )
                                                                            )

                                                                            val object1 =
                                                                                JSONObject(
                                                                                    response
                                                                                )
                                                                            val jsonArray1 =
                                                                                object1.getJSONArray(
                                                                                    Constant.DATA
                                                                                )
                                                                            val g1 =
                                                                                Gson()
                                                                            for (i in 0 until jsonArray1.length()) {
                                                                                val jsonObject1 =
                                                                                    jsonArray1.getJSONObject(
                                                                                        i
                                                                                    )
                                                                                if (jsonObject1 != null) {
                                                                                    val walletTransaction =
                                                                                        g1.fromJson(
                                                                                            jsonObject1.toString(),
                                                                                            WalletTransaction::class.java
                                                                                        )
                                                                                    walletTransactions.add(
                                                                                        walletTransaction
                                                                                    )
                                                                                } else {
                                                                                    break
                                                                                }
                                                                            }
                                                                            walletTransactionAdapter.notifyDataSetChanged()
                                                                            walletTransactionAdapter.setLoaded()
                                                                            isLoadMore =
                                                                                false
                                                                        }
                                                                    } catch (e: JSONException) {
                                                                        e.printStackTrace()
                                                                        binding.shimmerFrameLayout.stopShimmer()
                                                                        binding.shimmerFrameLayout.visibility =
                                                                            View.GONE
                                                                        binding.recyclerView.visibility =
                                                                            View.GONE
                                                                    }
                                                                }
                                                            }
                                                        },
                                                        activity,
                                                        Constant.TRANSACTION_URL,
                                                        params1,
                                                        false
                                                    )
                                                }
                                                isLoadMore =
                                                    true
                                            }
                                        }
                                    }
                                }
                            }
                        }else {
                            binding.recyclerView.visibility = View.GONE
                            binding.tvAlert.visibility = View.VISIBLE
                            binding.shimmerFrameLayout.stopShimmer()
                            binding.shimmerFrameLayout.visibility =
                                View.GONE
                            binding.recyclerView.visibility = View.GONE
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        binding.shimmerFrameLayout.stopShimmer()
                        binding.shimmerFrameLayout.visibility = View.GONE
                        binding.recyclerView.visibility = View.GONE
                    }
                } else {
                    binding.recyclerView.visibility = View.GONE
                    binding.tvAlert.visibility = View.VISIBLE
                    binding.shimmerFrameLayout.stopShimmer()
                    binding.shimmerFrameLayout.visibility =
                        View.GONE
                    binding.recyclerView.visibility = View.GONE
                }

            }
        }, activity, Constant.TRANSACTION_URL, params, false)
    }

    private fun createMidtransPayment(
        grossAmount: String?,
        sendParams: MutableMap<String, String>
    ) {
        val params: MutableMap<String, String> = HashMap()
        params[Constant.ORDER_ID] =
            "wallet-refill-user-" + Session(activity).getData(
                Constant.ID
            ) + "-" + System.currentTimeMillis()
        params[Constant.GROSS_AMOUNT] =
            "" + grossAmount!!.toDouble().roundToLong().toInt()
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(
                result: Boolean,
                response: String
            ) {
                if (result) {
                    try {
                        val jsonObject =
                            JSONObject(response)
                        if (!jsonObject.getBoolean(Constant.ERROR)) {
                            val intent = Intent(
                                activity,
                                MidtransActivity::class.java
                            )
                            intent.putExtra(
                                Constant.URL,
                                jsonObject.getJSONObject(
                                    Constant.DATA
                                ).getString(
                                    Constant.REDIRECT_URL
                                )
                            )
                            intent.putExtra(
                                Constant.ORDER_ID,
                                "wallet-refill-user-" + Session(
                                    activity
                                ).getData(
                                    Constant.ID
                                ) + "-" + System.currentTimeMillis()
                            )
                            intent.putExtra(
                                Constant.FROM,
                                Constant.WALLET
                            )
                            intent.putExtra(
                                Constant.PARAMS,
                                sendParams as Serializable?
                            )
                            startActivity(intent)
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
        }, activity, Constant.MIDTRANS_PAYMENT_URL, params, true)
    }

    override fun onResume() {
        super.onResume()
        Constant.TOOLBAR_TITLE =
            getString(R.string.wallet_history)
        Session.setCount(
            Constant.UNREAD_WALLET_COUNT,
            0,
            activity
        )
        activity.invalidateOptionsMenu()
        hideKeyboard()
    }

    fun hideKeyboard() {
        try {
            val inputMethodManager =
                (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            inputMethodManager.hideSoftInputFromWindow(
                root.applicationWindowToken,
                0
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun rechargeWallet(
        activity: Activity,
        dialog: AlertDialog,
        tvWarning: TextView
    ) {
        if (paymentMethod != "") {
            tvWarning.visibility = View.GONE
            val sendParams: MutableMap<String, String> = HashMap()
            if (paymentMethod == getString(
                    R.string.paypal
                )
            ) {
                sendParams[Constant.FINAL_TOTAL] =
                    amount
                sendParams[Constant.FIRST_NAME] =
                    session.getData(
                        Constant.NAME
                    ).toString()
                sendParams[Constant.LAST_NAME] =
                    session.getData(
                        Constant.NAME
                    ).toString()
                sendParams[Constant.PAYER_EMAIL] =
                    session.getData(
                        Constant.EMAIL
                    ).toString()
                sendParams[Constant.ITEM_NAME] =
                    getString(
                        R.string.wallet_recharge_
                    )
                sendParams[Constant.ITEM_NUMBER] =
                    System.currentTimeMillis()
                        .toString() + Constant.randomNumeric(
                        3
                    )
                startPaypalPayment(
                    sendParams
                )
            } else if (paymentMethod == "RazorPay") {
                createOrderId(
                    amount.toDouble()
                )
            } else if (paymentMethod == "Paystack") {
                sendParams[Constant.FINAL_TOTAL] =
                    amount
                sendParams[Constant.FROM] =
                    Constant.WALLET
                callPayStack(
                    sendParams
                )
            } else if (paymentMethod == "Flutterwave") {
                startFlutterWavePayment()
            } else if (paymentMethod == "Midtrans") {
                sendParams[Constant.FINAL_TOTAL] =
                    amount
                sendParams[Constant.USER_ID] =
                    session.getData(
                        Constant.ID
                    ).toString()
                createMidtransPayment(
                    amount,
                    sendParams
                )
            } else if (paymentMethod == "stripe") {
                if (Constant.DefaultAddress != "") {
                    sendParams[Constant.FINAL_TOTAL] =
                        amount
                    sendParams[Constant.USER_ID] =
                        session.getData(
                            Constant.ID
                        ).toString()

                    val intent = Intent(activity, StripeActivity::class.java)
                    intent.putExtra(
                        Constant.ORDER_ID,
                        "wallet-refill-user-" + Session(activity).getData(
                            Constant.ID
                        ) + "-" + System.currentTimeMillis()
                    )
                    intent.putExtra(Constant.FROM, Constant.WALLET)
                    intent.putExtra(Constant.PARAMS, hashSetOf(sendParams))
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        activity,
                        getString(
                            R.string.address_msg
                        ), Toast.LENGTH_SHORT
                    ).show()
                }
            } else if (paymentMethod == "PayTm") {
                startPayTmPayment()
            } else if (paymentMethod == "SSLCOMMERZ") {
                startSslCommerzPayment(
                    activity,
                    amount,
                    msg,
                    System.currentTimeMillis()
                        .toString() + Constant.randomNumeric(
                        3
                    )
                )
            }
            dialog.dismiss()
        } else {
            tvWarning.visibility = View.VISIBLE
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.toolbar_cart).isVisible =
            false
        menu.findItem(R.id.toolbar_layout).isVisible =
            false
        menu.findItem(R.id.toolbar_sort).isVisible =
            false
        menu.findItem(R.id.toolbar_search).isVisible =
            false
    }

    companion object {
        lateinit var amount: String
        lateinit var msg: String
        lateinit var paymentMethod: String
    }
}
