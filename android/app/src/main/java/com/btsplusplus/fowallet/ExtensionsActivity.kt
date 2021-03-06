package com.btsplusplus.fowallet

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Point
import android.net.Uri
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Scroller
import android.widget.Toast
import bitshares.*
import com.btsplusplus.fowallet.kline.TradingPair
import com.fowallet.walletcore.bts.BitsharesClientManager
import com.fowallet.walletcore.bts.ChainObjectManager
import com.fowallet.walletcore.bts.WalletManager
import kotlinx.android.synthetic.main.bottom_nav.*
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Field


fun AppCompatActivity.setFullScreen() {
    val dector_view: View = window.decorView
    val option: Int = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE

    dector_view.systemUiVisibility = option
    window.navigationBarColor = Color.TRANSPARENT
}

fun AppCompatActivity.setBottomNavigationStyle(position: Int) {
    val color: Int = resources.getColor(R.color.theme01_textColorHighlight)
    when (position) {
        0 -> {
            bottom_nav_text_view_markets.setTextColor(color)
            bottom_nav_image_view_markets.setColorFilter(color)
        }
        1 -> {
            bottom_nav_text_view_diya.setTextColor(color)
            bottom_nav_image_view_diya.setColorFilter(color)
        }
        2 -> {
            bottom_nav_text_view_services.setTextColor(color)
            bottom_nav_image_view_services.setColorFilter(color)
        }
        3 -> {
            bottom_nav_text_view_my.setTextColor(color)
            bottom_nav_image_view_my.setColorFilter(color)
        }
    }
    //  TODO:7.0 ??????????????????????????????????????????????????? singleTop????????? onNewIntent?????????
    if (BuildConfig.kAppModuleEnableTabMarket) {
        bottom_nav_markets_frame.visibility = View.VISIBLE
        bottom_nav_markets_frame.setOnClickListener {
            val top = BtsppApp.getInstance().getTopActivity()
            if (top == null || top !is ActivityIndexMarkets) {
                goTo(ActivityIndexMarkets::class.java)
                BtsppApp.getInstance().finishAllActivity()
            }
        }
    } else {
        bottom_nav_markets_frame.visibility = View.GONE
    }
    if (BuildConfig.kAppModuleEnableTabDebt) {
        bottom_nav_markets_frame.visibility = View.VISIBLE
        bottom_nav_diya_frame.setOnClickListener {
            val top = BtsppApp.getInstance().getTopActivity()
            if (top == null || top !is ActivityIndexCollateral) {
                goTo(ActivityIndexCollateral::class.java)
                BtsppApp.getInstance().finishAllActivity()
            }
        }
    } else {
        bottom_nav_diya_frame.visibility = View.GONE
    }
    bottom_nav_services_frame.setOnClickListener {
        val top = BtsppApp.getInstance().getTopActivity()
        if (top == null || top !is ActivityIndexServices) {
            goTo(ActivityIndexServices::class.java)
            BtsppApp.getInstance().finishAllActivity()
        }
    }
    bottom_nav_my_frame.setOnClickListener {
        val top = BtsppApp.getInstance().getTopActivity()
        if (top == null || top !is ActivityIndexMy) {
            goTo(ActivityIndexMy::class.java)
            BtsppApp.getInstance().finishAllActivity()
        }
    }
}

fun AppCompatActivity.clearBottomAllColor() {
    val default_color: Int = resources.getColor(R.color.theme01_textColorGray)
    //  ??????
    bottom_nav_text_view_markets.setTextColor(default_color)
    bottom_nav_text_view_diya.setTextColor(default_color)
    bottom_nav_text_view_services.setTextColor(default_color)
    bottom_nav_text_view_my.setTextColor(default_color)
    //  ??????
    bottom_nav_image_view_markets.setColorFilter(default_color)
    bottom_nav_image_view_diya.setColorFilter(default_color)
    bottom_nav_image_view_services.setColorFilter(default_color)
    bottom_nav_image_view_my.setColorFilter(default_color)
}

fun android.app.Activity.alerShowMessageConfirm(title: String?, message: String): Promise {
    return UtilsAlert.showMessageConfirm(this, title, message)
}

fun android.app.Activity.viewUserLimitOrders(account_id: String, tradingPair: TradingPair?) {
    //  [??????]
    btsppLogCustom("event_view_userlimitorders", jsonObjectfromKVS("account", account_id))

    val mask = ViewMask(R.string.kTipsBeRequesting.xmlstring(this), this)
    mask.show()
    //  1??????????????????
    val p1 = ChainObjectManager.sharedChainObjectManager().queryFullAccountInfo(account_id)

    //  2???????????????
    //  ??????????????? 100 ????????????
    val stop = "1.${EBitsharesObjectType.ebot_operation_history.value}.0"
    val start = "1.${EBitsharesObjectType.ebot_operation_history.value}.0"
    //  start - ?????????ID???????????????????????????ID?????????????????????ID???0????????????????????????????????????????????????????????? start???
    //  stop  - ??????????????????ID????????????????????????ID?????????????????????0???????????????????????????????????????or??????limit?????????????????????????????? stop ID???
    val conn = GrapheneConnectionManager.sharedGrapheneConnectionManager().any_connection()
    val p2 = conn.async_exec_history("get_account_history", jsonArrayfrom(account_id, stop, 100, start))

    //  ????????????
    Promise.all(p1, p2).then {
        val array_list = it as JSONArray

        val full_account_data = array_list.getJSONObject(0)
        val account_history = array_list.getJSONArray(1)

        //  ?????????
        val asset_id_hash = JSONObject()
        val limit_orders = full_account_data.optJSONArray("limit_orders")
        if (limit_orders != null && limit_orders.length() > 0) {
            for (order in limit_orders) {
                val sell_price = order!!.getJSONObject("sell_price")
                asset_id_hash.put(sell_price.getJSONObject("base").getString("asset_id"), true)
                asset_id_hash.put(sell_price.getJSONObject("quote").getString("asset_id"), true)
            }
        }

        //  ????????????
        val tradeHistory = JSONArray()
        for (history in account_history) {
            val op = history!!.getJSONArray("op")
            val op_code = op.getInt(0)
            if (op_code == EBitsharesOperations.ebo_fill_order.value) {
                tradeHistory.put(history)
                val op_info = op.getJSONObject(1)
                asset_id_hash.put(op_info.getJSONObject("pays").getString("asset_id"), true)
                asset_id_hash.put(op_info.getJSONObject("receives").getString("asset_id"), true)
            }
        }

        //  ?????? & ??????
        return@then ChainObjectManager.sharedChainObjectManager().queryAllAssetsInfo(asset_id_hash.keys().toJSONArray()).then {
            mask.dismiss()
            goTo(ActivityMyOrders::class.java, true, args = JSONObject().apply {
                put("full_account_data", full_account_data)
                put("trade_history", tradeHistory)
                put("tradingPair", tradingPair)
            })
            return@then null
        }
    }.catch {
        mask.dismiss()
        showToast(resources.getString(R.string.tip_network_error))
    }
}

fun android.app.Activity.viewUserAssets(account_name_or_id: String) {
    //  [??????]
    btsppLogCustom("event_view_userassets", jsonObjectfromKVS("account", account_name_or_id))

    val mask = ViewMask(R.string.kTipsBeRequesting.xmlstring(this), this)
    mask.show()

    val chainMgr = ChainObjectManager.sharedChainObjectManager()
    chainMgr.queryFullAccountInfo(account_name_or_id).then {
        val full_account_data = it as JSONObject
        val userAssetDetailInfos = OrgUtils.calcUserAssetDetailInfos(full_account_data)
        val args = userAssetDetailInfos.getJSONObject("validBalancesHash").keys().toJSONArray()
        return@then chainMgr.queryAllAssetsInfo(args).then {
            val bitasset_data_id_list = JSONArray()
            for (asset_id in args.forin<String>()) {
                val bitasset_data_id = chainMgr.getChainObjectByID(asset_id!!).optString("bitasset_data_id")
                if (bitasset_data_id.isNotEmpty()) {
                    bitasset_data_id_list.put(bitasset_data_id)
                }
            }
            return@then chainMgr.queryAllGrapheneObjects(bitasset_data_id_list).then {
                mask.dismiss()
                goTo(ActivityMyAssets::class.java, true, args = jsonArrayfrom(userAssetDetailInfos, full_account_data))
                return@then null
            }
        }
    }.catch {
        mask.dismiss()
        showToast(resources.getString(R.string.tip_network_error))
    }

}

fun android.app.Activity.runOnMainUI(body: () -> Unit) {
    this.runOnUiThread { body() }
}

/**
 * ???????????????
 */
fun android.app.Activity.hideSoftKeyboard() {
    val view = this.currentFocus
    if (view != null) {
        val mgr = this.getSystemService(android.app.Activity.INPUT_METHOD_SERVICE) as? InputMethodManager
        if (mgr != null) {
            mgr.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }
}

fun android.app.Activity.showToast(str: String, duration: Int = Toast.LENGTH_SHORT) {
    UtilsAlert.showToast(this.applicationContext, str, duration)
}

fun Fragment.showToast(str: String, duration: Int = Toast.LENGTH_SHORT) {
    this.activity?.showToast(str, duration)
}

/**
 * ???????????????????????????????????????????????????????????????
 */
fun android.app.Activity.showGrapheneError(error: Any?) {
    if (error != null) {
        if (error is String) {
            showToast(error)
            return
        }
        try {
            val json = if (error is Promise.WsPromiseException) {
                JSONObject(error.message.toString())
            } else {
                JSONObject(error.toString())
            }

            var msg = json.optString("message", "")
            val stack = json.optJSONObject("data")?.optJSONArray("stack")
            if (stack != null && stack.length() > 0) {
                val format = stack.optJSONObject(0)?.optString("format", null)
                if (format != null) {
                    msg = String.format("%s : %s", msg, format)
                }
            }

            if (msg != "") {
                //  ??????????????????
                //  "Assert Exception: account: no such account"
                if (msg.indexOf("no such account") >= 0) {
                    showToast(resources.getString(R.string.kGPErrorAccountNotExist))
                    return
                }
                if (msg.indexOf("Insufficient Balance") >= 0) {
                    showToast(resources.getString(R.string.kGPErrorInsufficientBalance))
                    return
                }
                //  "Preimage size mismatch." or ""Provided preimage does not generate correct hash."
                val lowermsg = msg.toLowerCase()
                if (lowermsg.indexOf("preimage size") >= 0 || lowermsg.indexOf("provided preimage") >= 0) {
                    showToast(resources.getString(R.string.kGPErrorRedeemInvalidPreimage))
                    return
                }
                if (lowermsg.indexOf("no method with") >= 0) {
                    showToast(resources.getString(R.string.kGPErrorApiNodeVersionTooLow))
                    return
                }
                if (lowermsg.indexOf("fee pool balance") >= 0) {
                    //  format = "core_fee_paid <= fee_asset_dyn_data->fee_pool: Fee pool balance of '${b}' is less than the ${r} required to convert ${c}";
                    showToast(resources.getString(R.string.kGPErrorFeePoolInsufficient))
                    return
                }
                //  REMARK?????????????????????????????????????????????????????????????????????????????????
                if (lowermsg.indexOf("itr != cidx.end()") >= 0) {
                    //    context = {
                    //        file = "confidential_evaluator.cpp";
                    //        hostname = "";
                    //        level = error;
                    //        line = 89;
                    //        method = "do_evaluate";
                    //        "thread_name" = "th_a";
                    //        timestamp = "2020-04-16T01:22:30";
                    //    };
                    //    data = {
                    //    };
                    //    format = "itr != cidx.end(): ";
                    //  REMARK???????????? message ??????????????????????????? context ???????????????
                    if (stack != null && stack.length() > 0) {
                        val file = stack.optJSONObject(0)?.optJSONObject("context")?.optString("file", null)
                        if (file != null && file.indexOf("confidential") >= 0) {
                            showToast(resources.getString(R.string.kGPErrorBlindReceiptIsNotExisted))
                            return
                        }
                    }
                }
                if (lowermsg.indexOf("fc::ecc::verify_sum") >= 0) {
                    showToast(resources.getString(R.string.kGPErrorBlindVerifySumFailed))
                    return
                }
                //  TODO:6.0 receive asset error - create limit order
                //  This market has not been whitelisted by the selling asset
                //  This market has been blacklisted by the selling asset

                //  The account is not allowed to transact the selling asset
                //  The account is not allowed to transact the receiving asset

                //  TODO:6.0 update asset
                //  "flags & white_list" - when account black or white list is not empty.

                //  Transaction exceeds maximum transaction size. TODO:8.0 ??????????????????????????????
                //  TODO:fowallet ???????????????????????????????????????
            }
        } catch (e: Exception) {
        }
    }
    //  ??????????????????
    showToast(resources.getString(R.string.tip_network_error))
}

fun Fragment.showGrapheneError(error: Any?) {
    this.activity?.showGrapheneError(error)
}

/**
 *  (private) ??????????????????
 */
fun android.app.Activity.onExecuteCreateProposalCore(opcode: EBitsharesOperations, opdata: JSONObject, opaccount: JSONObject, proposal_create_args: JSONObject, success_callback: (() -> Unit)?) {
    val fee_paying_account = proposal_create_args.getJSONObject("kFeePayingAccount")
    val fee_paying_account_id = fee_paying_account.getString("id")

    //  ??????
    val mask = ViewMask(R.string.kTipsBeRequesting.xmlstring(this), this)
    mask.show()
    BitsharesClientManager.sharedBitsharesClientManager().proposalCreate(opcode, opdata, opaccount, proposal_create_args).then {
        mask.dismiss()
        if (success_callback != null) {
            success_callback()
        } else {
            showToast(R.string.kProposalSubmitTipTxOK.xmlstring(this))
        }
        btsppLogCustom("txProposalCreateOK", jsonObjectfromKVS("opcode", opcode.value, "account", fee_paying_account_id))
        return@then null
    }.catch { err ->
        mask.dismiss()
        showGrapheneError(err)
        btsppLogCustom("txProposalCreateFailed", jsonObjectfromKVS("opcode", opcode.value, "account", fee_paying_account_id))
    }
}

/**
 *  (public)?????????????????????????????????????????????????????????
 */
fun android.app.Activity.askForCreateProposal(opcode: EBitsharesOperations, using_owner_authority: Boolean, invoke_proposal_callback: Boolean,
                                              opdata: JSONObject, opaccount: JSONObject,
                                              body: ((isProposal: Boolean, proposal_create_args: JSONObject) -> Unit)?, success_callback: (() -> Unit)?) {
    val account_name = opaccount.getString("name")
    var message: String
    if (using_owner_authority) {
        message = String.format(R.string.kProposalTipsAskMissingOwner.xmlstring(this), account_name)
    } else {
        message = String.format(R.string.kProposalTipsAskMissingActive.xmlstring(this), account_name)
    }
    alerShowMessageConfirm(resources.getString(R.string.kWarmTips), message).then {
        if (it != null && it as Boolean) {
            //  ????????????????????????
            val result_promise = Promise()
            val args = jsonObjectfromKVS("opcode", opcode, "opaccount", opaccount, "opdata", opdata, "result_promise", result_promise)
            goTo(ActivityCreateProposal::class.java, true, args = args)
            result_promise.then { result ->
                if (result != null) {
                    val proposal_create_args = result as? JSONObject
                    if (proposal_create_args != null) {
                        if (invoke_proposal_callback) {
                            body!!(true, proposal_create_args)
                        } else {
                            onExecuteCreateProposalCore(opcode, opdata, opaccount, proposal_create_args, success_callback)
                        }
                    }
                }
            }
        }
        return@then null
    }
}

/**
 *  (public) ???????????????????????????-???????????????????????????-?????????????????????????????????
 *  using_owner_authority - ????????????owner?????????????????????active?????????
 */
fun android.app.Activity.GuardProposalOrNormalTransaction(opcode: EBitsharesOperations, using_owner_authority: Boolean, invoke_proposal_callback: Boolean,
                                                          opdata: JSONObject, opaccount: JSONObject,
                                                          body: (isProposal: Boolean, fee_paying_account: JSONObject?) -> Unit) {
    val permission_json = if (using_owner_authority) opaccount.getJSONObject("owner") else opaccount.getJSONObject("active")
    if (WalletManager.sharedWalletManager().canAuthorizeThePermission(permission_json)) {
        //  ????????????
        body(false, null)
    } else {
        //  ?????????????????????????????????????????????
        askForCreateProposal(opcode, using_owner_authority, invoke_proposal_callback, opdata, opaccount, body, null)
    }
}

/**
 * ??????????????????????????????????????????????????????????????????
 */
fun android.app.Activity.guardWalletUnlocked(checkActivePermission: Boolean, body: (unlocked: Boolean) -> Unit) {
    val walletMgr = WalletManager.sharedWalletManager()
    if (walletMgr.isLocked()) {
        val title = if (walletMgr.getWalletMode() == AppCacheManager.EWalletMode.kwmPasswordOnlyMode.value) R.string.unlockTipsUnlockAccount.xmlstring(this) else R.string.unlockTipsUnlockWallet.xmlstring(this)
        val placeholder = when (walletMgr.getWalletMode()) {
            //  ????????????
            AppCacheManager.EWalletMode.kwmPasswordOnlyMode.value -> resources.getString(R.string.unlockTipsPleaseInputAccountPassword)
            //  ????????????
            AppCacheManager.EWalletMode.kwmPasswordWithWallet.value -> resources.getString(R.string.kLoginTipsPlaceholderTradePassword)
            AppCacheManager.EWalletMode.kwmPrivateKeyWithWallet.value -> resources.getString(R.string.kLoginTipsPlaceholderTradePassword)
            AppCacheManager.EWalletMode.kwmBrainKeyWithWallet.value -> resources.getString(R.string.kLoginTipsPlaceholderTradePassword)
            //  ????????????
            AppCacheManager.EWalletMode.kwmFullWalletMode.value -> resources.getString(R.string.registerLoginPagePleaseInputWalletPws)
            else -> resources.getString(R.string.kLoginImportTipsPleaseInputPassword)
        }
        UtilsAlert.showInputBox(this, title, placeholder, resources.getString(R.string.unlockBtnUnlock)).then {
            val password = it as? String
            if (password == null) {
                body(false)
            } else if (password == "") {
                showToast(resources.getString(R.string.kMsgPasswordCannotBeNull))
            } else {
                val unlockInfos = WalletManager.sharedWalletManager().unLock(password, this)
                var unlockSuccess = unlockInfos.getBoolean("unlockSuccess")
                if (unlockSuccess && checkActivePermission && !unlockInfos.optBoolean("haveActivePermission")) {
                    unlockSuccess = false
                }
                if (unlockSuccess) {
                    body(true)
                } else {
                    showToast(unlockInfos.getString("err"))
                    body(false)
                }
            }
        }
    } else {
        body(true)
    }
}

/**
 * ?????????????????????????????????????????????????????????REMARK?????????????????????????????????????????????????????????????????????
 */
fun android.app.Activity.guardWalletUnlocked(body: (unlocked: Boolean) -> Unit) {
    guardWalletUnlocked(true, body)
}

/**
 * ?????????????????????????????????????????????????????????
 */
fun android.app.Activity.guardWalletExist(body: () -> Unit) {
    if (WalletManager.sharedWalletManager().isWalletExist()) {
        body()
    } else {
        goTo(ActivityLogin::class.java, true)
    }
}

/**
 *  (public) ????????????????????????????????????????????????REMARK???????????????????????????????????????????????????
 */
fun android.app.Activity.guardWalletExistWithWalletMode(message: String, body: () -> Unit) {
    guardWalletExist {
        if (WalletManager.sharedWalletManager().isPasswordMode()) {
            alerShowMessageConfirm(resources.getString(R.string.kWarmTips), message).then {
                if (it != null && it as Boolean) {
                    val result_promise = Promise()
                    goTo(ActivityUpgradeToWalletMode::class.java, true, args = jsonObjectfromKVS("result_promise", result_promise))
                    result_promise.then {
                        if (it != null && it as Boolean) {
                            body()
                        }
                    }
                }
                return@then null
            }
        } else {
            body()
        }
    }
}

/**
 * ??????????????? full_account_data ???????????????????????????????????? asset ????????????????????????
 */
fun android.app.Activity.get_full_account_data_and_asset_hash(account_name_or_id: String): Promise {
    //  TODO:??????????????? ChainObjectManager???
    return ChainObjectManager.sharedChainObjectManager().queryFullAccountInfo(account_name_or_id).then {
        val full_account_data = it as JSONObject
        val list = JSONArray()
        for (balance in full_account_data.getJSONArray("balances")) {
            list.put(balance!!.getString("asset_type"))
        }
        return@then ChainObjectManager.sharedChainObjectManager().queryAllAssetsInfo(list).then {
            //  (void)asset_hash ?????????????????? ChainObjectManager ?????????
            return@then full_account_data
        }
    }
}

/**
 * ????????????
 */
fun AppCompatActivity.goHome() {
    val home = Intent(Intent.ACTION_MAIN)
    home.addCategory(Intent.CATEGORY_HOME)
    startActivity(home)
}

/**
 * ??????webview??????
 */
fun android.app.Activity.goToWebView(title: String, url: String) {
    goTo(ActivityWebView::class.java, true, args = arrayOf(title, url))
}

/**
 * ?????????????????????????????????
 */
fun android.app.Activity.openURL(url: String) {
    try {
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    } catch (e: Exception) {
        //  TODO:??????URL?????????
    }
}

/**
 *  (public) ????????????
 *      REMARK???back???????????????finish?????????finish???????????????activity???back???????????????????????????????????????????????????????????????????????????????????????????????????????????????
 */
fun android.app.Activity.goTo(cls: Class<*>, transition_animation: Boolean = false, back: Boolean = false, args: Any? = null, request_code: Int = -1, clear_navigation_stack: Boolean = false) {
    val intent = Intent()
    intent.setClass(this, cls)

    if (back) {
        //  ????????????????????? Activity
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        //  ?????????????????????????????????????????????????????????????????????activity???
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

    //  ????????????
    if (args != null) {
        intent.putExtra(BTSPP_START_ACTIVITY_PARAM_ID, ParametersManager.sharedParametersManager().genParams(args))
    }

    //  ??????????????????
    if (request_code > 0) {
        startActivityForResult(intent, request_code)
    } else {
        startActivity(intent)
    }

    if (!transition_animation) {
        overridePendingTransition(0, 0)
    }

    //  ?????????????????????REMARK?????????????????????????????? Activity ??? onCreate ???????????????????????????????????????????????????
    if (clear_navigation_stack) {
        BtsppApp.getInstance().finishActivityToNavigationTop()
    }
}

/**
 * ????????????????????????????????????
 */
fun android.app.Activity.isHaveNavigationBar(): Boolean {
    val display = this.windowManager.defaultDisplay
    val size = Point()
    val realsize = Point()
    //  ???????????????
    display.getSize(size)
    //  ???????????????????????????
    display.getRealSize(realsize)
    return size.y != realsize.y
}

/**
 * ??????????????????????????? contentView
 */
fun AppCompatActivity.setAutoLayoutContentView(layoutResID: Int, navigationBarColor: Int? = null) {
    setContentView(layoutResID)
    adjustWindowSizeForNavigationBar(navigationBarColor)
    //  [??????]
    btsppLogCustom("setAutoLayoutContentView", jsonObjectfromKVS("activity", this::class.java.name))
}

/**
 * ????????????????????????
 */
fun android.app.Activity.adjustWindowSizeForNavigationBar(navigationBarColor: Int? = null) {
    val display = this.windowManager.defaultDisplay
    val size = Point()
    val realsize = Point()
    display.getSize(size)
    display.getRealSize(realsize)
    if (size.y != realsize.y) {
        val contentView = findViewById<View>(android.R.id.content)
        //  ???????????????????????????????????????????????????
        contentView.layoutParams.height = size.y
        //  ????????????????????????????????????
        if (navigationBarColor != null) {
            contentView.rootView?.setBackgroundColor(resources.getColor(navigationBarColor))
        } else {
            contentView.rootView?.setBackgroundColor(resources.getColor(R.color.theme01_appBackColor))
        }
    }
}

fun AppCompatActivity.toDp(v: Float): Int {
    return Utils.toDp(v, this.resources)
}

class ViewPagerAdapter(fm: FragmentManager, _fragmets: ArrayList<Fragment>) : FragmentPagerAdapter(fm) {

    val fragments: ArrayList<Fragment> = _fragmets

    override fun getItem(p0: Int): Fragment {
        return fragments[p0]
    }

    override fun getCount(): Int {
        return fragments.size
    }
}

class ViewPagerScroller(context: Context?, interpolator: OvershootInterpolator) : Scroller(context) {

    var mDuration: Int = 0

    fun setDuration(_mDuration: Int) {
        mDuration = _mDuration
    }

    override fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int) {
        super.startScroll(startX, startY, dx, dy, this.mDuration)
    }

    override fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int, duration: Int) {
        super.startScroll(startX, startY, dx, dy, this.mDuration)
    }
}

fun AppCompatActivity.setViewPager(default_select_index: Int, view_pager_id: Int, tablayout_id: Int, fragmens: ArrayList<Fragment>) {
    val _view_pager = findViewById<ViewPager>(view_pager_id)
    _view_pager.adapter = ViewPagerAdapter(supportFragmentManager, fragmens)

    val f: Field = ViewPager::class.java.getDeclaredField("mScroller")
    f.isAccessible = true
    val vpc = ViewPagerScroller(_view_pager.context, OvershootInterpolator(0.6f))
    f.set(_view_pager, vpc)
    vpc.duration = 700

    //  default selected
    val _tablayout = findViewById<TabLayout>(tablayout_id)
    _tablayout.getTabAt(default_select_index)!!.select()
    _view_pager.currentItem = default_select_index

    _view_pager.setOnPageChangeListener(object : ViewPager.OnPageChangeListener {
        override fun onPageScrollStateChanged(state: Int) {
        }

        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        }

        override fun onPageSelected(position: Int) {
            _tablayout.getTabAt(position)!!.select()
        }
    })
}

fun AppCompatActivity.setTabListener(tablayout_id: Int, view_pager_id: Int, tab_clicked: ((pos: Int) -> Unit)? = null) {
    val _view_pager = findViewById<ViewPager>(view_pager_id)
    findViewById<TabLayout>(tablayout_id).setOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab) {
            val pos = tab.position
            _view_pager.setCurrentItem(pos, true)
            if (tab_clicked != null) {
                tab_clicked(pos)
            }
        }

        override fun onTabUnselected(tab: TabLayout.Tab) {
            //tab???????????????????????????
        }

        override fun onTabReselected(tab: TabLayout.Tab) {
            //tab???????????????????????????
        }
    })
}
