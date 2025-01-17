package com.px.mgr.controller.xcx;

import com.px.db.custom.service.OilMinPriceService;
import com.px.db.model.bean.*;
import com.px.db.model.service.*;
import com.px.db.util.QueryHelper;
import com.px.db.util.QueryModel;
import com.px.db.util.SqlSortModel;
import com.px.util.MyExistUtil;
import com.px.web.service.AdvertHelpService;
import com.px.web.service.ApiHelpService;
import com.px.web.service.XcxHelpService;
import com.px.web.controller.BaseController;
import com.px.web.service.XcxPayHelpService;
import com.px.web.util.ResponseResultUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Controller
@RequestMapping("/api")
public class PageController extends BaseController {
    @Resource
    private UserService userService;
    @Resource
    private XcxHelpService xcxHelpService;
    @Resource
    private XcxSettingsService xcxSettingsService;
    @Resource
    private BannerService bannerService;
    @Resource
    private OilMinPriceService oilMinPriceService;
    @Resource
    private SettingsService settingsService;
    @Resource
    private XcxPayHelpService xcxPayHelpService;
    @Resource
    private ArticleService articleService;
    @Resource
    private AdvertService advertService;
    @Resource
    private ApiHelpService apiHelpService;
    @Resource
    private AdvertHelpService advertHelpService;

    /**
     * 接口测试 正式环境请删除
     *
     * @param id
     * @return
     */
    @RequestMapping("/test")
    @ResponseBody
    public ResponseResultUtil test(Integer id) {
        xcxPayHelpService.updateUserIntegral(id);
        return ResponseResultUtil.success();
    }

    /**
     * 首页头部导航数据获取
     *
     * @return
     */
    @RequestMapping("/wxapp/base")
    @ResponseBody
    public ResponseResultUtil wxappBase() {
        log.info("-------------------首页数据加载------------------------------");
        //授权成功返回的数据
        Map<String, Object> resultMap = new HashMap<>();
        Map<String, Object> map = new HashMap<>();
        List<XcxSettings> xcxSettingsList = xcxSettingsService.selectAll();
        XcxSettings xcxSettings = new XcxSettings();
        if (xcxSettingsList.size() > 0) {
            xcxSettings = xcxSettingsList.get(0);
        }
        Map<String, Object> wxapp = new HashMap<>();
        wxapp.put("is_service", xcxSettings.getIsService());
        wxapp.put("is_phone", xcxSettings.getIsPhone());
        wxapp.put("phone_no", xcxSettings.getPhoneNo());
        Map<String, Object> navbar = new HashMap<>();
        navbar.put("id", xcxSettings.getId());
        navbar.put("wxapp_title", xcxSettings.getWxappTitle());
        navbar.put("top_background_color", xcxSettings.getTopBackgroundColor());
        Map<String, Object> topTextColor = new HashMap<>();
        topTextColor.put("text", xcxSettings.getTopTextColor());
        topTextColor.put("value", xcxSettings.getTopFontSize());
        navbar.put("top_text_color", topTextColor);
        wxapp.put("navbar", navbar);
        map.put("wxapp", wxapp);
        resultMap.put("data", map);

        return ResponseResultUtil.success(resultMap);
    }


    /**
     * 轮播图数据获取
     *
     * @return
     */
    @RequestMapping("/index/page")
    @ResponseBody
    public ResponseResultUtil indexPage() {
        log.info("-------------------轮播图数据加载------------------------------");
        //授权成功返回的数据
        Map<String, Object> resultMap = new HashMap<>();


        Map<String, Object> dataMap = new HashMap<>();
        /*
           ----------------------------未设置小程序链接 测试获取所有轮播图----------------------------------------
         */
        List<Banner> bannerList = bannerService.selectEqualsT(new Banner().setXcxShow(true));
        List<Map<String, Object>> items = new ArrayList<>();
        int index = 1;
        Map<String, Object> map = new HashMap<>();
        map.put("id", index);
        map.put("type", "banner");
        Map<String, Object> style = new HashMap<>();
        style.put("btnColor", "#ffffff");
        style.put("btnColor", "round");
        map.put("style", style);
        List<Map<String, Object>> datas = new ArrayList<>();
        for (Banner banner : bannerList) {
            Map<String, Object> bannerMap = new HashMap<>();
            if (MyExistUtil.notEmptyString(banner.getImg())) {
                bannerMap.put("imgUrl", xcxHelpService.getUrl() + banner.getImg());
                bannerMap.put("imgName", xcxHelpService.getUrl() + banner.getImg());
            }
            bannerMap.put("linkUrl", banner.getXcxPageUrl());
            datas.add(bannerMap);
        }
        map.put("data", datas);
        XcxSettings xcxSettings = xcxSettingsService.selectByPrimaryKey(1);
        if (xcxSettings == null) {
            xcxSettings = new XcxSettings().setShowNotice((byte) 0);
        }
        dataMap.put("showNotice", xcxSettings.getShowNotice());
        dataMap.put("noticeContent", xcxSettings.getNoticeContent());
        items.add(map);
        dataMap.put("items", items);
        //未设置shop_info
        dataMap.put("shop_info", new ArrayList<>());
        Map minPriceMap = oilMinPriceService.getOilMinPrice("#92");
        dataMap.put("min_price", minPriceMap.get("price"));
        resultMap.put("data", dataMap);

        return ResponseResultUtil.success(resultMap);
    }


    @RequestMapping("/index/banner")
    @ResponseBody
    public ResponseResultUtil banner() {
        log.info("-------------------轮播图数据加载------------------------------");

        List<Banner> bannerList = bannerService.selectEqualsT(new Banner().setAppShow(true));
        for (Banner banner : bannerList) {
            if (MyExistUtil.notEmptyString(banner.getImg())) {
                banner.setImg(xcxHelpService.getUrl() + banner.getImg());
            }
            if (MyExistUtil.notEmptyString(banner.getAppPageUrl())) {
                banner.setAppPageUrl(xcxHelpService.getH5Url() + banner.getAppPageUrl());
            }

        }
        return ResponseResultUtil.success(bannerList);
    }


    /**
     * 小程序获取广告和链接
     *
     * @return
     */
    @RequestMapping("/user/getAdv")
    @ResponseBody
    public ResponseResultUtil getAdv() {
        log.info("-------------------获取广告图片和链接------------------------------");
        advertHelpService.updateAdvertStatus();
        Map<String, Object> map = advertHelpService.getAdvertByPosition("user_middle", "xcx");
        map.put("advertising_blg", map.get("image"));
        map.put("advertising_url", map.get("url"));
        return ResponseResultUtil.success(map);
    }

    @RequestMapping("/user/getAgreement")
    @ResponseBody
    public ResponseResultUtil getAgreement() {
        log.info("-------------------获取用户协议------------------------------");
        Map<String, Object> map = new HashMap<>();

        Article userAgreementArticle = articleService.selectOneByProperty("type", "user_agreement");
        String userAgreement = "";
        if (userAgreementArticle != null) {
            userAgreement = userAgreementArticle.getContent();
        }
        Article privacyAgreementArticle = articleService.selectOneByProperty("type", "privacy_agreement");
        String privacyAgreement = "";
        if (userAgreementArticle != null) {
            privacyAgreement = privacyAgreementArticle.getContent();
        }
        map.put("user", userAgreement);
        map.put("privacy", privacyAgreement);
        return ResponseResultUtil.success(map);
    }
}
