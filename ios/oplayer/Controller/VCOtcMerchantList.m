//
//  VCOtcMerchantList.m
//  oplayer
//
//  Created by SYALON on 13-10-23.
//
//

#import "VCOtcMerchantList.h"
#import "VCOtcOrders.h"
#import "VCOtcUserAuthInfos.h"
#import "VCOtcReceiveMethods.h"

#import "ViewOtcAdInfoCell.h"
#import "MBProgressHUDSingleton.h"
#import "OtcManager.h"

@interface VCOtcMerchantListPages ()
{
    NSString*   _curr_asset_name;
    EOtcAdType  _default_ad_type;
}

@end

@implementation VCOtcMerchantListPages

-(void)dealloc
{
}

- (id)initWithAssetName:(NSString*)asset_name ad_type:(EOtcAdType)ad_type
{
    self = [super init];
    if (self) {
        // Custom initialization
        _curr_asset_name = asset_name;
        _default_ad_type = ad_type;
    }
    return self;
}

- (NSInteger)getTitleDefaultSelectedIndex
{
    return _default_ad_type == eoadt_user_buy ? 1 : 2;
}

- (NSArray*)getTitleStringArray
{
    return @[NSLocalizedString(@"kOtcPageTitleBuy", @"我要买"), NSLocalizedString(@"kOtcPageTitleSell", @"我要卖")];
}

- (NSArray*)getSubPageVCArray
{
    return @[[[VCOtcMerchantList alloc] initWithOwner:self ad_type:eoadt_user_buy],
             [[VCOtcMerchantList alloc] initWithOwner:self ad_type:eoadt_user_sell]];
}

- (void)onRightOrderButtonClicked
{
    [[OtcManager sharedOtcManager] guardUserIdVerified:self
                                             auto_hide:YES
                                     askForIdVerifyMsg:NSLocalizedString(@"kOtcAdAskIdVerifyTips01", @"在继续操作之前您需要先完成身份认证，是否继续？")
                                              callback:^(id auth_info)
    {
        VCBase* vc = [[VCOtcOrders alloc] initWithAuthInfo:auth_info];
        [self pushViewController:vc vctitle:NSLocalizedString(@"kVcTitleOtcOrderList", @"订单记录") backtitle:kVcDefaultBackTitleName];
    }];
}

- (void)onRightUserButtonClicked
{
    [[MyPopviewManager sharedMyPopviewManager] showActionSheet:self
                                                       message:nil
                                                        cancel:NSLocalizedString(@"kBtnCancel", @"取消")
                                                         items:@[NSLocalizedString(@"kOtcAdUserActionItemAuthInfo", @"认证信息"), NSLocalizedString(@"kOtcAdUserActionItemReceiveMethod", @"收款方式")]
                                                      callback:^(NSInteger buttonIndex, NSInteger cancelIndex)
     {
         if (buttonIndex != cancelIndex){
             switch (buttonIndex) {
                 case 0:    //  认证信息
                 {
                     [[OtcManager sharedOtcManager] guardUserIdVerified:self
                                                              auto_hide:YES
                                                      askForIdVerifyMsg:nil
                                                               callback:^(id auth_info)
                     {
                         VCBase* vc = [[VCOtcUserAuthInfos alloc] initWithAuthInfo:auth_info];
                         [self pushViewController:vc
                                          vctitle:NSLocalizedString(@"kVcTitleOtcAuthInfos", @"认证信息")
                                        backtitle:kVcDefaultBackTitleName];
                     }];
                 }
                     break;
                 case 1:    //  收款方式
                 {
                     [[OtcManager sharedOtcManager] guardUserIdVerified:self
                                                              auto_hide:YES
                                                      askForIdVerifyMsg:NSLocalizedString(@"kOtcAdAskIdVerifyTips02", @"添加收款方式之前，请先完成身份认证，是否继续？")
                                                               callback:^(id auth_info)
                     {
                         VCBase* vc = [[VCOtcReceiveMethods alloc] initWithAuthInfo:auth_info];
                         [self pushViewController:vc
                                          vctitle:NSLocalizedString(@"kVcTitleOtcReceiveMethodsList", @"收款方式")
                                        backtitle:kVcDefaultBackTitleName];
                     }];
                 }
                     break;
                 default:
                     break;
             }
         }
     }];
}

- (NSString*)genTitleString
{
    return [NSString stringWithFormat:@"%@%@", _curr_asset_name, NSLocalizedString(@"kOtcAdTitleBase", @"市场◢")];
}

- (void)onTitleAssetButtonClicked:(UIButton*)sender
{
    id list = [[OtcManager sharedOtcManager].asset_list_digital ruby_map:^id(id src) {
        return [src objectForKey:@"assetSymbol"];
    }];
    [[MyPopviewManager sharedMyPopviewManager] showActionSheet:self
                                                       message:NSLocalizedString(@"kOtcAdSwitchAssetTips", @"请选择要交易的资产")
                                                        cancel:NSLocalizedString(@"kBtnCancel", @"取消")
                                                         items:list
                                                      callback:^(NSInteger buttonIndex, NSInteger cancelIndex)
     {
         if (buttonIndex != cancelIndex){
             id asset_name = [list objectAtIndex:buttonIndex];
             if (![_curr_asset_name isEqualToString:asset_name]) {
                 _curr_asset_name = asset_name;
                 [sender updateTitleWithoutAnimation:[self genTitleString]];
                 [self queryCurrentPageAdList];
             }
         }
     }];
}

- (void)viewDidLoad
{
    [super viewDidLoad];
	// Do any additional setup after loading the view.
    ThemeManager* theme = [ThemeManager sharedThemeManager];
    
    self.view.backgroundColor = theme.appBackColor;
    
    //  导航栏右边 订单和个人信息按钮
    id btn1 = [self naviButtonWithImage:@"iconOtcOrder" action:@selector(onRightOrderButtonClicked) color:theme.textColorNormal];
    id btn2 = [self naviButtonWithImage:@"iconOtcUser" action:@selector(onRightUserButtonClicked) color:theme.textColorNormal];
    [self.navigationItem setRightBarButtonItems:@[btn2, btn1]];
    
    //  导航栏中间标题
    UIButton* btnAssetBtn = [UIButton buttonWithType:UIButtonTypeSystem];
    btnAssetBtn.titleLabel.font = [UIFont boldSystemFontOfSize:17];
    [btnAssetBtn setTitle:[self genTitleString] forState:UIControlStateNormal];
    [btnAssetBtn setTitleColor:[ThemeManager sharedThemeManager].textColorMain forState:UIControlStateNormal];
    btnAssetBtn.userInteractionEnabled = YES;
    [btnAssetBtn addTarget:self action:@selector(onTitleAssetButtonClicked:) forControlEvents:UIControlEventTouchUpInside];
    btnAssetBtn.frame = CGRectMake(0, 0, self.view.bounds.size.width, [self heightForNavigationBar]);
    self.navigationItem.titleView = btnAssetBtn;
    
    //  查询数据
    [self queryCurrentPageAdList];
}

- (void)queryCurrentPageAdList
{
    VCOtcMerchantList* vc = (VCOtcMerchantList*)[self currentPage];
    if (vc) {
        [vc queryAdList:_curr_asset_name];
    }
}

- (void)onPageChanged:(NSInteger)tag
{
    NSLog(@"onPageChanged: %@", @(tag));
    
    //  gurad
    if ([[MBProgressHUDSingleton sharedMBProgressHUDSingleton] is_showing]){
        return;
    }
    
    //  query
    if (_subvcArrays){
        VCOtcMerchantList* vc = [_subvcArrays safeObjectAtIndex:tag - 1];
        if (vc){
            [vc queryAdList:_curr_asset_name];
        }
    }
}

@end

@interface VCOtcMerchantList ()
{
    __weak VCBase*      _owner;         //  REMARK：声明为 weak，否则会导致循环引用。
    
    EOtcAdType          _ad_type;       //  用户买入界面（商家卖出）
    UITableViewBase*    _mainTableView;
    UILabel*            _lbEmpty;
    
    NSMutableArray*     _data_array;
}

@end

@implementation VCOtcMerchantList

-(void)dealloc
{
    if (_mainTableView){
        [[IntervalManager sharedIntervalManager] releaseLock:_mainTableView];
        _mainTableView.delegate = nil;
        _mainTableView = nil;
    }
    _lbEmpty = nil;
    _data_array = nil;
    _owner = nil;
}

- (id)initWithOwner:(VCBase*)owner ad_type:(EOtcAdType)ad_type
{
    self = [super init];
    if (self) {
        // Custom initialization
        _ad_type = ad_type;
        _owner = owner;
        _data_array = [NSMutableArray array];
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    self.view.backgroundColor = [ThemeManager sharedThemeManager].appBackColor;
    
    // Do any additional setup after loading the view.
    CGRect rect = [self rectWithoutNaviAndPageBar];
    
    _mainTableView = [[UITableViewBase alloc] initWithFrame:rect style:UITableViewStyleGrouped];
    _mainTableView.delegate = self;
    _mainTableView.dataSource = self;
    _mainTableView.backgroundColor = [UIColor clearColor];
    _mainTableView.separatorStyle = UITableViewCellSeparatorStyleNone;  //  REMARK：不显示cell间的横线。
    [self.view addSubview:_mainTableView];

    _lbEmpty = [self genCenterEmptyLabel:rect txt:NSLocalizedString(@"kOtcAdNoAnyMerchantOnline", @"没有任何商家在线。")];
    [self.view addSubview:_lbEmpty];
}

/*
 *  (private) 查询广告列表
 */
- (void)queryAdList:(NSString*)asset_name
{
    //  TODO:2.9 page args
    [_owner showBlockViewWithTitle:NSLocalizedString(@"kTipsBeRequesting", @"请求中...")];
    [[[[OtcManager sharedOtcManager] queryAdList:_ad_type asset_name:asset_name page:0 page_size:50] then:^id(id data) {
        [_owner hideBlockView];
        [self onQueryAdListResponsed:data];
        return nil;
    }] catch:^id(id error) {
        [_owner hideBlockView];
        [[OtcManager sharedOtcManager] showOtcError:error];
        return nil;
    }];
}

- (void)onQueryAdListResponsed:(id)responsed
{
    //  TODO:2.9 这里显示异常？
    assert([[responsed objectForKey:@"code"] integerValue] == 0);
    
    id list = [[responsed objectForKey:@"data"] objectForKey:@"records"];
    [_data_array removeAllObjects];
    if (list && [list isKindOfClass:[NSArray class]]) {
        [_data_array addObjectsFromArray:list];
    }
    
    //  动态设置UI的可见性
    if ([_data_array count] > 0){
        _mainTableView.hidden = NO;
        _lbEmpty.hidden = YES;
        [_mainTableView reloadData];
    }else{
        _mainTableView.hidden = YES;
        _lbEmpty.hidden = NO;
    }
}

#pragma mark- TableView delegate method
- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
    return 1;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    return [_data_array count];
}

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath
{
    CGFloat baseHeight = 8 + 24 + 4 + 20 * 2 + 40 + 8;
    
    return baseHeight;
}

/**
 *  调整Header和Footer高度。REMARK：header和footer VIEW 不能为空，否则高度设置无效。
 */
- (CGFloat)tableView:(UITableView *)tableView heightForHeaderInSection:(NSInteger)section
{
    return 8.0f;
}
- (nullable NSString *)tableView:(UITableView *)tableView titleForHeaderInSection:(NSInteger)section
{
    return @" ";
}

- (CGFloat)tableView:(UITableView *)tableView heightForFooterInSection:(NSInteger)section
{
    return 8.0f;
}
- (nullable NSString *)tableView:(UITableView *)tableView titleForFooterInSection:(NSInteger)section
{
    return @" ";
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    static NSString* identify = @"id_otc_ad_cell";
    ViewOtcAdInfoCell* cell = (ViewOtcAdInfoCell*)[tableView dequeueReusableCellWithIdentifier:identify];
    if (!cell)
    {
        cell = [[ViewOtcAdInfoCell alloc] initWithStyle:UITableViewCellStyleValue1 reuseIdentifier:identify vc:self];
        cell.selectionStyle = UITableViewCellSelectionStyleNone;
        cell.accessoryType = UITableViewCellAccessoryNone;
        cell.backgroundColor = [UIColor clearColor];
    }
    cell.showCustomBottomLine = YES;
    cell.adType = _ad_type;
    [cell setTagData:indexPath.row];
    [cell setItem:[_data_array objectAtIndex:indexPath.row]];
    return cell;
}

-(void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    [tableView deselectRowAtIndexPath:indexPath animated:YES];
}

/*
 *  (private) 账号异常，冻结时，是否联系客服。
 */
- (void)askForContactCustomerService:(id)auth_info
{
    [[UIAlertViewManager sharedUIAlertViewManager] showCancelConfirm:NSLocalizedString(@"kOtcAdUserFreezeAsk", @"您的账号已被冻结，是否联系客服？")
                                                           withTitle:NSLocalizedString(@"kWarmTips", @"温馨提示")
                                                          completion:^(NSInteger buttonIndex)
     {
         if (buttonIndex == 1)
         {
             // TODO:2.9 客服界面是什么样的...
         }
     }];
}

/*
 *  (private) 用户的收款方式和商家付款方式不匹配的提示。
 */
- (void)askForAddNewPaymentMethod:(id)aditem auth_info:(id)auth_info
{
    BOOL bankcardPaySwitch = [[aditem objectForKey:@"bankcardPaySwitch"] boolValue];
    BOOL aliPaySwitch = [[aditem objectForKey:@"aliPaySwitch"] boolValue];
    BOOL wechatPaySwitch = NO; //  TODO:2.9 默认false，ad数据里没微信。
    
    NSMutableArray* ary = [NSMutableArray array];
    if (aliPaySwitch) {
        [ary addObject:NSLocalizedString(@"kOtcAdPmNameAlipay", @"支付宝")];
    }
    if (bankcardPaySwitch) {
        [ary addObject:NSLocalizedString(@"kOtcAdPmNameBankCard", @"银行卡")];
    }
    if (wechatPaySwitch) {
        [ary addObject:NSLocalizedString(@"kOtcAdPmNameWechatPay", @"微信支付")];
    }
    assert([ary count] > 0);
    NSString* paymentStrList = [ary componentsJoinedByString:NSLocalizedString(@"kOtcAdPmJoinChar", @"、")];
    [[UIAlertViewManager sharedUIAlertViewManager] showCancelConfirm:[NSString stringWithFormat:NSLocalizedString(@"kOtcAdOrderMissingPmAsk", @"商家仅支持通过【%@】向您付款，您需要前往添加并激活对应收款方式，是否继续？"), paymentStrList]
                                                           withTitle:NSLocalizedString(@"kWarmTips", @"温馨提示")
                                                          completion:^(NSInteger buttonIndex)
     {
         if (buttonIndex == 1)
         {
             VCBase* vc = [[VCOtcReceiveMethods alloc] initWithAuthInfo:auth_info];
             [_owner pushViewController:vc
                                vctitle:NSLocalizedString(@"kVcTitleOtcReceiveMethodsList", @"收款方式")
                              backtitle:kVcDefaultBackTitleName];
         }
     }];
}

/*
 *  (private) 价格变化，是否继续下单?
 */
- (void)askForPriceChanged:(id)ad_info lock_info:(id)lock_info
{
    [[UIAlertViewManager sharedUIAlertViewManager] showCancelConfirm:NSLocalizedString(@"kOtcAdOrderPriceChangeAsk", @"您当前选择的订单价格有变动，是否继续下单？")
                                                           withTitle:NSLocalizedString(@"kWarmTips", @"温馨提示")
                                                          completion:^(NSInteger buttonIndex)
     {
         if (buttonIndex == 1)
         {
             [self gotoInputOrderCore:ad_info lock_info:lock_info];
         }
     }];
}

/*
 *  (private) 前往下单
 */
- (void)gotoInputOrderCore:(id)ad_info lock_info:(id)lock_info
{
    [[[MyPopviewManager sharedMyPopviewManager] showOtcTradeView:_owner ad_info:ad_info lock_info:lock_info] then:^id(id result) {
        if (result) {
            //  输入完毕：尝试下单
            [_owner showBlockViewWithTitle:NSLocalizedString(@"kTipsBeRequesting", @"请求中...")];
            OtcManager* otc = [OtcManager sharedOtcManager];
            //TODO:2.9 [lock_info objectForKey:@"legalCurrencySymbol"]
            [[[otc createUserOrder:[otc getCurrentBtsAccount]
                             ad_id:[ad_info objectForKey:@"adId"]
                              type:(EOtcAdType)[[ad_info objectForKey:@"adType"] integerValue]
                             price:[NSString stringWithFormat:@"%@", lock_info[@"unitPrice"]]
                             total:result[@"total"]] then:^id(id responsed) {
                [_owner hideBlockView];
                //  TODO:2.9
                [OrgUtils makeToast:@"order done!"];
                return nil;
            }] catch:^id(id error) {
                [_owner hideBlockView];
                [otc showOtcError:error];
                return nil;
            }];
        }
        return nil;
    }];
}

/*
 *  (private) 用户卖出时 - 查询用户的收款方式列表。
 */
- (WsPromise*)_queryPaymentMethodList
{
    if (_ad_type == eoadt_user_buy) {
        return [WsPromise resolve:@YES];
    } else {
        OtcManager* otc = [OtcManager sharedOtcManager];
        return [[otc queryPaymentMethods:[otc getCurrentBtsAccount]] then:^id(id payment_responsed) {
            return [payment_responsed objectForKey:@"data"];
        }];
    }
}

/*
 *  事件 - 点击购买or出售按钮。
 */
- (void)onButtonBuyOrSellClicked:(UIButton*)sender
{
    assert(sender.tag < [_data_array count]);
    id item = [_data_array objectAtIndex:sender.tag];
    assert(item);
        
    BOOL bankcardPaySwitch = [[item objectForKey:@"bankcardPaySwitch"] boolValue];
    BOOL aliPaySwitch = [[item objectForKey:@"aliPaySwitch"] boolValue];
    BOOL wechatPaySwitch = NO; //  TODO:2.9 默认false，ad数据里没微信。
  
    //  TODO:2.9临时关闭check
    if (!bankcardPaySwitch && !aliPaySwitch && !wechatPaySwitch) {
        if (_ad_type == eoadt_user_buy) {
            //  用户买
            [OrgUtils makeToast:NSLocalizedString(@"kOtcAdOrderTipsMcNoAnyReceiveMethod", @"商家没开启任何收款方式。")];
        } else {
            //  用户卖
            [OrgUtils makeToast:NSLocalizedString(@"kOtcAdOrderTipsMcNoAnyPaymentMethod", @"商家没开启任何付款方式。")];
        }
        return;
    }
    
    id adId = [item objectForKey:@"adId"];
    assert(adId);
    
    [_owner GuardWalletUnlocked:YES body:^(BOOL unlocked) {
        if (unlocked) {
            OtcManager* otc = [OtcManager sharedOtcManager];
            [otc guardUserIdVerified:_owner
                           auto_hide:NO
                   askForIdVerifyMsg:NSLocalizedString(@"kOtcAdAskIdVerifyTips03", @"您尚未完成身份认证，不可进行场外交易，是否去认证？")
                            callback:^(id auth_info)
            {
                //  1、查询账号状态：用户账号是否异常
                if ([[auth_info objectForKey:@"status"] integerValue] == eous_freeze) {
                    [_owner hideBlockView];
                    [self askForContactCustomerService:auth_info];
                    return;
                }
                
                //  2、仅针对用户卖出的情况：收款方式和商家付款方式是否匹配 REMARK：用户买入不用check，只要商家开启任意收款方式即可。
                [[[self _queryPaymentMethodList] then:^id(id pminfo_list) {
                    //  仅卖出的情况
                    if (_ad_type == eoadt_user_sell) {
                        BOOL bPaymentMatch = NO;
                        if (pminfo_list && [pminfo_list isKindOfClass:[NSArray class]] && [pminfo_list count] > 0) {
                            for (id pminfo in pminfo_list) {
                                if ([[pminfo objectForKey:@"status"] integerValue] == eopms_enable) {
                                    NSInteger pmtype = [pminfo[@"type"] integerValue];
                                    switch (pmtype) {
                                        case eopmt_alipay:
                                        {
                                            if (aliPaySwitch) {
                                                bPaymentMatch = YES;
                                            }
                                        }
                                            break;
                                        case eopmt_bankcard:
                                        {
                                            if (bankcardPaySwitch) {
                                                bPaymentMatch = YES;
                                            }
                                        }
                                            break;
                                        case eopmt_wechatpay:
                                        {
                                            if (wechatPaySwitch) {
                                                bPaymentMatch = YES;
                                            }
                                        }
                                            break;
                                        default:
                                            break;
                                    }
                                    //  已匹配
                                    if (bPaymentMatch) {
                                        break;
                                    }
                                }
                            }
                        }
                        if (!bPaymentMatch) {
                            [_owner hideBlockView];
                            [self askForAddNewPaymentMethod:item auth_info:auth_info];
                            return nil;
                        }
                    }
                    //  3、锁定价格&前往下单（TODO:2.9 是否先查询广告详情，目前数据一直）
                    return [[otc lockPrice:[otc getCurrentBtsAccount]
                                     ad_id:adId
                                      type:(EOtcAdType)[[item objectForKey:@"adType"] integerValue]
                              asset_symbol:[item objectForKey:@"assetSymbol"]
                                     price:[item objectForKey:@"price"]] then:^id(id data) {
                        [_owner hideBlockView];
                        id lock_info = [data objectForKey:@"data"];
                        assert(lock_info);
                        NSString* oldprice = [NSString stringWithFormat:@"%@", [item objectForKey:@"price"]];
                        NSString* newprice = [NSString stringWithFormat:@"%@", [lock_info objectForKey:@"unitPrice"]];
                        //  价格变化
                        if (![oldprice isEqualToString:newprice]) {
                            [self askForPriceChanged:item lock_info:lock_info];
                        } else {
                            [self gotoInputOrderCore:item lock_info:lock_info];
                        }
                        return nil;
                    }];
                }] catch:^id(id error) {
                    [_owner hideBlockView];
                    [otc showOtcError:error];
                    return nil;
                }];
            }];
        }
    }];
}

@end
