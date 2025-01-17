let App = getApp();
var is_no_more = !1,
  is_loading = !1,
  p = 2;

Page({

  /**
   * 页面的初始数据
   */
  data: {
    dataType: 'price',
    list: [],
    amount:0,
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad: function (options) {
    
  
    this.data.dataType = options.type || 'price';
    this.setData({
      dataType: this.data.dataType
    });
  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow: function () {
    is_no_more = !1,
      is_loading = !1,
      p = 2;
    if (!App.checkIsLogin()) {
      App.showError('请先授权登录', function () {
        wx.navigateTo({
          url: '../login/login',
        });
      });
      return;
    }
    // 获取订单列表
    this.getList(this.data.dataType);
  },

  /**
   * 获取订单列表
   */
  getList: function (dataType) {
    wx.showLoading({
      title: '加载中...',
    })
    let _this = this;
    _this.setData({list:[]});
    if(dataType=='price'){
      App._get1('wallet/getUserIntegral', {        
        'page': 1
      }, function (result) {
        wx.hideLoading()
        console.info(result.data);
        _this.setData({list:result.data.list});
        _this.setData({amount:result.data.amount});

        result.data.length && wx.pageScrollTo({
          scrollTop: 0
        });
      });
    }else{
      App._get1('wallet/getUserIntegralLog', {
       
        'page': 1
      }, function (result) {

        wx.hideLoading()
        _this.setData({list:result.data});
        result.data.length && wx.pageScrollTo({
          scrollTop: 0
        });
      });
    }
  
  },


  onReachBottom: function () {

    let _this = this;
    if (is_loading == 0 && is_no_more == 0) {

      is_loading = 1;
      var type= _this.data.dataType;
      if(type=='price'){
        wx.request({
          url: App.api_root + 'wallet/getUserIntegral',
          data: {
            'token': wx.getStorageSync('token'),
  
            'page': p
          },
          success: function (t) {
            console.info(t);
            if (200 == t.data.status) {
              //将已有数据和分页新查询的数据合并
              var order_list = _this.data.list.concat(t.data.data.list);
              _this.setData({
                list: order_list
              });
              //如果没有数据，标记为“没有数据”
              if (t.data.data.list.length == 0) {
                is_no_more = 1;
  
              }
              p++;
            }
          },
          complete: function () {
            is_loading = 0;
          }
        })
      }else{
        wx.request({
          url: App.api_root + 'wallet/getUserIntegralLog',
          data: {
            'token': wx.getStorageSync('token'),
            'dataType': _this.data.dataType,
            'page': p
          },
          success: function (t) {
            if (200 == t.data.status) {
              //将已有数据和分页新查询的数据合并
              var order_list = _this.data.list.concat(t.data.data);
              _this.setData({
                list: order_list
              });
              //如果没有数据，标记为“没有数据”
              if (t.data.data.length == 0) {
                is_no_more = 1;
  
              }
              p++;
            }
          },
          complete: function () {
            is_loading = 0;
          }
        })
      }
    
    }
  },

  /**
   * 切换标签
   */
  bindHeaderTap: function (e) {
    console.log(e);
    this.setData({
      dataType: e.currentTarget.dataset.type
    });
    is_no_more = !1, is_loading = !1, p = 2;
    // 获取订单列表
    this.getList(e.currentTarget.dataset.type);
  },

  emptyFunc: function () {},
  onUnload: function () {
    wx.getStorage({
      key: 'flag',
      success(res) {
        if (res.data == "true") {
          wx.switchTab({
            url: '/pages/user/index',
          })
        }
      }
    })

  },
  onPullDownRefresh: function () {
    wx.stopPullDownRefresh();
  }


});