<template>
	<view>
		<text class="title">就诊须知</text>
		<text class="desc">发布单位：飞马市三医院</text>
		<image :src="bannerUrl" mode="widthFix" class="banner"></image>
		<u-parse :content="content" :tagStyle="style"></u-parse>
		<view class="operate"><u-button size="large" type="primary" @click="acceptHandle">我已知晓</u-button></view>
	</view>
</template>

<script>
export default {
	data() {
		return {
			bannerUrl: '/static/medical/hospital-banner.jpg',
			content: `
				<h3>1.就诊提醒：</h3>
				<p>
					我院门诊严格执行实名预约挂号就诊制度，就诊患者请携带本人有效身份证件（身份证、医保卡等）。
				</p>
				<h3>2.温馨提示：</h3>
				<p>
					目前门诊就诊患者较多，一位患者只能一位陪同，请陪同人员完成预检筛查。并出示身份证等有效证件配合流调人员的工作，必要时做好行程轨迹查询，确保安全就诊。谢谢您的配合！
				</p>
			`,
			style: {
				h3: `
					color:#444444;
					margin:30rpx 45rpx 0 45rpx;
					line-height:1.8;
				`,
				p: `
				color:#444444;
				margin:10rpx 45rpx 10rpx 45rpx;
				line-height:1.8;
				`
			}
		};
	},
	methods: {
		acceptHandle: function() {
			 let that = this;
			 //检查用户是否登陆了
			 let token = uni.getStorageSync('token');
			 if (token == null || token.length == 0) {
				 uni.showToast({
					 icon: 'error',
					 title: '请先登录小程序'
				 });
				 setTimeout(function() {
					 uni.switchTab({
					 url: '/pages/mine/mine'
					 });
				 }, 2000);
			 }
			 //检查用户是否创建了就诊卡
			 that.ajax(that.api.hasUserInfoCard,'GET',{},function(resp) {
				 let result = resp.data.result;
				 if (result) {
					  uni.redirectTo({
						url: '/registration/medical_dept_list/medical_dept_list'
					  });
				  } else {
					  uni.showToast({
						  icon: 'error',
						  title: '请先创建就诊卡'
					  });
					  setTimeout(function() {
						  uni.switchTab({
						  url: '/pages/mine/mine'
						  });
					  }, 2000);
				  }
				},false);
		}	},
	onLoad: function() {
		
	}
};
</script>

<style lang="less">
@import url(notice.less);
</style>
