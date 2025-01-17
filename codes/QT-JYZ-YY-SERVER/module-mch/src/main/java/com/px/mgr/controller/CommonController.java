package com.px.mgr.controller;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageInfo;
import com.px.db.model.bean.FileGroup;
import com.px.db.model.bean.FileList;
import com.px.db.model.bean.PageList;
import com.px.db.model.bean.Region;
import com.px.db.model.service.*;
import com.px.db.util.QueryHelper;
import com.px.db.util.QueryModel;
import com.px.db.util.SqlSortModel;
import com.px.util.MyExistUtil;
import com.px.web.controller.BaseController;
import com.px.web.redis.service.RedisService;
import com.px.web.util.ResponseResultUtil;
import com.px.web.util.login.LoginUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.File;
import java.util.*;

@RequestMapping("tools")
@Controller
@Slf4j
public class CommonController extends BaseController {
    @Resource
    private RegionService regionService;
    @Resource
    RedisService redisService;
    @Resource
    private PageListService pageListService;
    @Resource
    private FileListService fileListService;
    @Resource
    private FileGroupService fileGroupService;
    @Resource
    private SettingsService settingsService;


    @RequestMapping("/getRegionList")
    @ResponseBody
    public ResponseResultUtil region(Region search) {
        List<Region> regions = regionService.selectEqualsT(search);
        return ResponseResultUtil.success(regions);
    }

    @RequestMapping("/saveHideField")
    public void saveHideField(String name,String fields) {
        Integer adminId = LoginUtil.getIdCache();
        if(MyExistUtil.notEmptyString(name)){
            redisService.setData("hide_field_"+adminId+"_"+name,fields);
        }
    }

    @RequestMapping("/page_pick.html")
    public String pagePick(ModelMap model, Integer pageId, String pageUrl, String openType) {
//        PageList pageList=new PageList().setShowOnPick(true);

        System.out.println(pageUrl);
        String[] split = {};
        if(MyExistUtil.notEmptyString(openType)){

            split = openType.split(",");

//            pageList.setOpenType(openType);
        }
        model.put("values","");

        if(MyExistUtil.notEmptyString(pageUrl)){
            String[] params=pageUrl.split("\\?");
            if(params.length>1){
                String param=params[1];
                String[] strings=param.split("&");
                List<Map<String,Object>>  mapList=new ArrayList<>();
                for(int i=0;i<strings.length;i++){
                    Map<String,Object>  map=new HashMap<>();
                    String[] strings1=strings[i].split("=");
                    map.put("name",strings1[0]);
                    if(strings1.length>1){
                        map.put("value",strings1[1]);
                    }else{
                        map.put("value","");
                    }
                    mapList.add(map);
                }
                if(mapList.size()>0){
                    model.put("values", JSON.toJSONString(mapList));
                }
            }

        }

        QueryHelper.SimpleQueryHandler searches = QueryHelper.startSimpleQuery();
        searches.addQuery(new QueryModel(PageList.OPEN_TYPE_PROPERTY,split,QueryModel.IN_VALUES));
        searches.addQuery(new QueryModel(PageList.SHOW_ON_PICK_PROPERTY,true,QueryModel.EQUALS));
        List<PageList> pageLists = pageListService.selectByQuery(searches, new SqlSortModel(PageList.SORT_ORDER_FIELD));
//        List<PageList> pageLists = pageListService.selectEqualsT(pageList,new SqlSortModel(PageList.SORT_ORDER_FIELD));
        model.put("pageLists",pageLists);

        //已选中的链接
        PageList page=new PageList();
        if(MyExistUtil.isGreaterThan0Integer(pageId)){
            page = pageListService.selectByPrimaryKey(pageId);
            if(page != null){
            }else{
                page=new PageList();
                pageId = 0;
            }
        }
        model.put("param","");
        model.put("pageUrl",pageUrl);
        if(MyExistUtil.notEmptyString(page.getParams())){
            model.put("param",page.getParams());
        }
        model.put("pageId",pageId);
        model.put("item",page);
        return "common/page_pick";
    }


    /**
     * 图库页面
     * @param modelMap
     * @return
     */
    @RequestMapping("/image_pick.html")
    public String imagePick(ModelMap modelMap) {
        List<FileGroup> fileGroupList = getImgPickGroup();
        modelMap.put("fileGroupList",fileGroupList);
        return "common/image_pick";
    }

    /**
     * 上传图片至文件库
     * @param url 图片路径
     * @param groupId 分组id
     * @return
     */
    @ResponseBody
    @RequestMapping("/imagePickUpload")
    public ResponseResultUtil imagePickUpload(String url, Integer groupId) {
        if (null==url||"".equals(url)){
            return ResponseResultUtil.lose("操作失败");
        }
        //处理数据
        FileList fileList = new FileList();
        fileList.setUrl(url);
        //文件扩展名
        String extension = "";
        if (url.lastIndexOf(".") != -1 && url.lastIndexOf(".") != 0) {
            extension = url.substring(url.lastIndexOf(".") + 1);
        }
        fileList.setExtension(extension);
        fileList.setType("image");
        //文件大小
        File f = new File(url);
        if (f.exists() && f.isFile()){
            fileList.setSize((int) f.length());
        }
        if (null!=groupId){
            fileList.setGroupId(groupId);
        }
        //新增
        boolean insertFlag = fileListService.insertOne(fileList);
        if (insertFlag){
            //插入日志
            boolean insertLog = insertAddLog(new FileList(),"文件格式："+fileList.getType()+"，文件路径："+fileList.getUrl(),fileList.getId());
            if (insertLog){
                return ResponseResultUtil.success();
            }
        }
        return ResponseResultUtil.lose("操作失败");
    }


    /**
     * 获取分组下的图片
     * @param groupId 分组id
     * @param page
     * @param limit
     * @return
     */
    @ResponseBody
    @RequestMapping("/getGroupFile")
    public ResponseResultUtil getGroupFile(Integer groupId,Integer page,Integer limit) {
        List<FileList> imagesList = new ArrayList<>();
        QueryHelper.SimpleQueryHandler searches = QueryHelper.startSimpleQuery();
        searches.addQuery(new QueryModel(FileList.TYPE_PROPERTY, "image"));
        if (null != groupId){
            searches.addQuery(new QueryModel(FileList.GROUP_ID_PROPERTY, groupId));
        }
        PageInfo<FileList> pageInfo =  fileListService.selectByQueryWithPage(searches,page,limit ,new SqlSortModel("id", true));
        imagesList = pageInfo.getList();
        return ResponseResultUtil.success(imagesList);
    }


    /**
     * 编辑/新增分组页面
     * @param modelMap
     * @return
     */
    @RequestMapping("/editGroup.html")
    public String editGroup(ModelMap modelMap,Integer id) {
        FileGroup fileGroup = new FileGroup();
        if (null != id) {
            fileGroup = fileGroupService.selectByPrimaryKey(id);
        }
        modelMap.put("item", fileGroup);
        return "common/edit_group";
    }

    /**
     * 编辑/新增分组
     * @param title 分组标题
     * @param openType 操作类型 edit/add
     * @param id 分组id
     * @return
     */
    @ResponseBody
    @RequestMapping("/addGroup")
    public ResponseResultUtil addGroup(String title,String openType,Integer id) {
        //判空
        if (null==title||"".equals(title)){
            return ResponseResultUtil.lose("操作失败");
        }
        if (null == openType||"".equals(openType)){
            return ResponseResultUtil.lose("操作失败");
        }
        //操作类型
        if (openType.equals("add")){
            boolean insertFlag = fileGroupService.insertOne(new FileGroup().setTitle(title));
            if (insertFlag){
                boolean insertLog = insertAddLog(new FileGroup(),title,null);
                if (insertLog){
                    List<FileGroup> fileGroupList = getImgPickGroup();
                    return ResponseResultUtil.success(fileGroupList);
                }
            }
        }else {
            //判空
            if (null==id){
                return ResponseResultUtil.lose("请选择要修改分组");
            }
            if (null==title||"".equals(title)){
                return ResponseResultUtil.lose("分组标题不能为空");
            }
            //判断数据是否存在
            FileGroup fileGroup = fileGroupService.selectByPrimaryKey(id);
            if (null==fileGroup){
                return ResponseResultUtil.lose("操作失败，数据不存在或已被删除");
            }
            if (title.equals(fileGroup.getTitle())){
                return ResponseResultUtil.lose("分组标题修改前后一致");
            }
            //修改分组
            boolean updateFlag = fileGroupService.updateByPrimaryKey(new FileGroup().setId(id).setTitle(title));
            if (updateFlag){
                //插入日志
                boolean insertLog = insertOneFieldModifyLog(FileGroup.TABLE_NAME,FileGroup.TITLE_PROPERTY,id,fileGroup.getTitle(),fileGroup.getTitle(),title);
                if (insertLog){
                    List<FileGroup> fileGroupList = getImgPickGroup();
                    return ResponseResultUtil.success(fileGroupList);
                }
            }
        }
        return ResponseResultUtil.lose("操作失败");
    }

    /**
     * 批量删除文件库图片
     * @param ids 图片id
     * @return
     */
    @ResponseBody
    @RequestMapping("/imagePickDel")
    public ResponseResultUtil imagePickUpload(String ids) {
        //判空
        if (null==ids||ids.equals("")){
            return ResponseResultUtil.lose("请选择要删除的图片");
        }
        //处理数据
        List<Integer> imageIds = new ArrayList<>();
        if (ids.contains(",")){
            String[] idsStr = ids.split(",");
            for (String i:idsStr){
                imageIds.add(Integer.valueOf(i));
            }
        } else {
            imageIds.add(Integer.valueOf(ids));
        }
        //判断数据是否存在
        List<String> title = new ArrayList<>();
        for (Integer id : imageIds){
            FileList fileList = fileListService.selectByPrimaryKey(id);
            if (fileList == null){
                return ResponseResultUtil.lose("操作失败，数据不存在或已被删除");
            }
            title.add("文件类型："+fileList.getType()+"，文件路径"+fileList.getUrl());
        }
        //删除数据
        Integer deleteInt = fileListService.deleteBatchInProperty("id",imageIds);
        if (deleteInt > 0){
            //插入日志
            boolean insertLog = insertBatchDelLog(new FileList(),title,imageIds);
            if (insertLog){
                return  ResponseResultUtil.success();
            }
        }
        return ResponseResultUtil.lose("操作失败");
    }


    /**
     * 删除分组及分组下的所有图片
     * @param id 分组id
     * @return
     */
    @ResponseBody
    @RequestMapping("/deleteGroup")
    public ResponseResultUtil deleteGroup(Integer id) {
        //判空
        if (null==id){
            return ResponseResultUtil.lose("请选择要删除分组");
        }
        //判断数据是否存在
        FileGroup fileGroup = fileGroupService.selectByPrimaryKey(id);
        if (null==fileGroup){
            return ResponseResultUtil.lose("数据不存在或已被删除");
        }
        //删除分组
        boolean deleteFlag = fileGroupService.deleteByPrimaryKey(id);
        if (deleteFlag){
            //插入日志
            boolean delLog = insertDelLog(new FileGroup(),fileGroup.getTitle(),id);
            if (delLog){
                //删除分组下所有图片
                List<FileList> fileLists = fileListService.selectEqualsT(new FileList().setGroupId(id).setType("image"));
                List<Integer> ids = new ArrayList<>();
                List<String> title = new ArrayList<>();

                if (fileLists.size()>0){
                    for (FileList f:fileLists){
                        ids.add(f.getId());
                        title.add("文件类型："+f.getType()+"，文件路径："+f.getUrl());
                    }
                    Integer deleteBatchNum = fileListService.deleteBatchInProperty("id",ids);
                    if (deleteBatchNum>0){
                        //插入日志
                        boolean insertBatchLog = insertBatchDelLog(new FileList(),title,ids);
                        if (insertBatchLog){
                            //搜索分组列表，返回重载分组列表
                            List<FileGroup> fileGroupList = getImgPickGroup();
                            return ResponseResultUtil.success(fileGroupList);
                        }
                    }
                }else{
                    //搜索分组列表，返回重载分组列表
                    List<FileGroup> fileGroupList = getImgPickGroup();
                    return ResponseResultUtil.success(fileGroupList);
                }
            }
        }
        return ResponseResultUtil.lose("操作失败");
    }

    /**
     * 图片移动分组页面
     * @param modelMap
     * @return
     */
    @RequestMapping("/move_img.html")
    public String moveImgInput(ModelMap modelMap) {
        //获取所有的分组
        List<FileGroup> fileGroupList = getImgPickGroup();
        modelMap.put("fileGroupList",fileGroupList);
        return "common/move_img";
    }

    /**
     * 图片移动分组
     * @param ids 图片id
     * @param groupId 分组id
     * @return
     */
    @ResponseBody
    @RequestMapping("/moveImg")
    public ResponseResultUtil moveImg(String ids,Integer groupId) {
        //判空
        if (null==ids||ids.equals("")){
            return ResponseResultUtil.lose("请选择要移动的图片");
        }
        if (null==groupId){
            return ResponseResultUtil.lose("请选择图片要移动的分组");
        }
        //处理数据
        List<FileList> fileListList = new ArrayList<>();
        if (ids.contains(",")){
            String[] idsStr = ids.split(",");
            for (String i:idsStr){
                fileListList.add(new FileList().setId(Integer.valueOf(i)));
            }
        } else {
            fileListList.add(new FileList().setId(Integer.valueOf(ids)));
        }

        //判断数据是否存在
        List<String> title = new ArrayList<>();
        List<Integer> idsLog = new ArrayList<>();
        List<String> oldValues = new ArrayList<>();

        //0为未分组
        if (groupId!=0){
            FileGroup fileGroup = fileGroupService.selectByPrimaryKey(groupId);
            if (null==fileGroup){
                return ResponseResultUtil.lose("操作失败，数据不存在或已被删除");
            }
        }

        for (FileList file : fileListList){
            FileList fileList = fileListService.selectByPrimaryKey(file.getId());
            if (fileList == null){
                return ResponseResultUtil.lose("操作失败，数据不存在或已被删除");
            }
            file.setGroupId(groupId);
            title.add("文件类型："+fileList.getType()+"，文件路径："+fileList.getUrl()+",文件分组："+fileList.getGroupId());
            idsLog.add(file.getId());
            oldValues.add(file.getGroupId()+"");
        }

        //修改数据
        boolean updateBatchFlag = fileListService.updateBatch(fileListList);
        if (updateBatchFlag){
            //插入日志
            boolean log = insertBatchModifyLog(FileList.TABLE_NAME,FileList.GROUP_ID_PROPERTY,title,idsLog,oldValues,groupId+"");
            if (log){
                List<FileGroup> fileGroupList = getImgPickGroup();
                return ResponseResultUtil.success(fileGroupList);
            }
        }
        return ResponseResultUtil.lose("操作失败");
    }

    /**
     * 获取文件库图片
     * @param ids 图片id
     * @return
     */
    @ResponseBody
    @RequestMapping("/getImg")
    public ResponseResultUtil getImg(String ids) {
        //判空
        if (null==ids||ids.equals("")){
            return ResponseResultUtil.lose("操作失败");
        }
        //处理数据
        List<Integer> imageIds = new ArrayList<>();
        if (ids.contains(",")){
            String[] idsStr = ids.split(",");
            for (String i:idsStr){
                imageIds.add(Integer.valueOf(i));
            }
        } else {
            imageIds.add(Integer.valueOf(ids));
        }
        //获取数据
        List<FileList> fileListList = new ArrayList<>();
        for (Integer id : imageIds){
            FileList fileList = fileListService.selectByPrimaryKey(id);
            if (fileList == null){
                return ResponseResultUtil.lose("操作失败，数据不存在或已被删除");
            }
            fileListList.add(fileList);
        }
        return ResponseResultUtil.success(fileListList);
    }


    /**
     * 获取所有分组，包含未分组
     * @return
     */
    private List<FileGroup> getImgPickGroup(){
        //获取所有分组
        //分组
        QueryHelper.SimpleQueryHandler searches = QueryHelper.startSimpleQuery();
        List<FileGroup> fileGroupList = fileGroupService.selectByQuery(searches, new SqlSortModel("id", true));
        //未分组
        List<FileList> noGroupFiles = fileListService.selectEqualsT(new FileList().setGroupId(0));
        if (noGroupFiles.size()>0){
            fileGroupList.add(new FileGroup().setTitle("未分组").setId(0).setSortOrder(0));
        }
        Collections.sort(fileGroupList, new Comparator<FileGroup>() {
            @Override
            public int compare(FileGroup fileGroup, FileGroup t1) {
                int diff = fileGroup.getId() - t1.getId();
                if (diff > 0) {
                    return 1;
                }else if (diff < 0) {
                    return -1;
                }
                return 0;
            }
        }); // 按id排序
        return fileGroupList;
    }


    /**
     * 腾讯地图定位
     * @param modelMap
     * @param lng  经度
     * @param lat  纬度
     * @return
     */
    @RequestMapping("/map_qq_location.html")
    public String qqMapLocationInput(ModelMap modelMap,String lng,String lat) {
        //获取所有的分组
        String coord = "";
        if(MyExistUtil.notEmptyString(lng) && MyExistUtil.notEmptyString(lat)){
            coord = lng+","+lat;
        }
        modelMap.put("coord",coord);
        modelMap.put("mapKey",settingsService.selectOneByProperty("code","map_qq_key").getValue());

        return "common/map_qq_location";
    }


//    /**
//     * 打印订单
//     */
//    @RequestMapping("printOrder")
//    @ResponseBody
//    public ResponseResultUtil printOrder(Integer id){
//
//        boolean flag = commonHelpService.orderPrint(id);
//
//        return ResponseResultUtil.success(flag);
//    }

}
