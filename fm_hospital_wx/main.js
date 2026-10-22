import App from './App'

// #ifndef VUE3
import Vue from 'vue'
Vue.config.productionTip = false

//使用uView组件库
import uView from 'uview-ui'
Vue.use(uView)

App.mpType = 'app'
const app = new Vue({
	...App
})
app.$mount()
// #endif

// #ifdef VUE3
// import {
// 	createSSRApp
// } from 'vue'
// export function createApp() {
// 	const app = createSSRApp(App)
// 	return {
// 		app
// 	}
// }
// #endif

//图片服务器地址
let minioUrl = "http://192.168.144.134:9000/hospital"
Vue.prototype.minioUrl = minioUrl

//图片服务器地址
let patientUrl = "http://124.222.182.113:9000/hospital/doctor"
Vue.prototype.patientUrl = patientUrl

//项目后端地址：
let baseUrl = "http://192.168.144.134:9092/wx"
// let baseUrl = "http://192.168.2.32:9092/wx"


Vue.prototype.tencent = {
	trtc: {
		appid: "OqM2fL3mYICHUJ4lccbjUSkFrbQXm2Mh"
	}
}



Vue.prototype.api = {
	loginOrRegister: baseUrl + "/patient/user/loginOrRegister",
	insertUserInfoCard: baseUrl + "/patient/user/info/insert",
	searchUserInfo:baseUrl + "/patient/user/selectUserInfo",
	searchUserInfoCard: baseUrl + "/patient/user/info/selectPatientUserInfoByUserId",
	updateUserInfoCard: baseUrl + "/patient/user/info/update",
	hasUserInfoCard: baseUrl + "/patient/user/info/checkPatientExistsByUserId",
	searchMedicalDeptList: baseUrl + "/medical/dept/selectMedicalDeptList",
	searchMedicalDeptSubList: baseUrl + "/medical/dept/sub/selectMedicalDeptSubList",
	searchCanRegisterInDateRange: baseUrl + "/doctor/work/plan/selectDoctorScheduleBySubIdAndDate",
	searchDeptSubDoctorPlanInDay: baseUrl + "/doctor/work/plan/getDoctorShiftDetailsBySubIdAndDate",
	checkRegisterCondition: baseUrl + "/patient/registration/checkRegisterCondition",
	createFaceModel: baseUrl + "/face/auth/createFaceModel",
	verifyFaceModel: baseUrl + "/face/auth/verifyFaceModel",
	searchDoctorWorkPlanSchedule: baseUrl + "/doctor/work/plan/selectDoctorWorkPlanSchedule",
	searchDoctorInfoById: baseUrl + "/doctor/selectDoctorInfoById",
	registerMedicalAppointment: baseUrl + "/patient/registration/registerMedicalAppointment",
	searchRegisterPaymentResult: baseUrl + "/patient/registration/selectPaymentResult",
	searchRegistrationByPage: baseUrl + "/patient/registration/selectRegistrationByPage",
	repayRegistration: baseUrl + "/patient/registration/repayRegistration",
	searchRegistrationInfo: baseUrl + "/patient/registration/selectRegistrationInfo",
	searchDeptAndSub: baseUrl + "/medical/dept/selectDeptAndSub",
	searchOnlineDoctorList: baseUrl + "/video/diagnosis/selectOnlineDoctorList",
	createVideoDiagnose: baseUrl + "/video/diagnosis/createVideoDiagnose",
	searchVideoDiagnosePaymentResult: baseUrl + "/video/diagnosis/searchPaymentResult",
	uploadVideoDiagnoseImage: baseUrl + "/video/diagnosis/uploadImage", 
	searchImageByVideoDiagnoseId: baseUrl + "/video/diagnosis/searchImageByVideoDiagnoseId", 
	deleteVideoDiagnoseImage: baseUrl + "/video_diagnose/deleteImage",
	searchRoomId: baseUrl + "/video_diagnose/searchRoomId",
	searchUserSig: baseUrl + "/video_diagnose/searchUserSig",
	searchPrescriptionByRegistrationId: baseUrl + "/consultation/report/selectConsultationReportByRegistrationId",





}

Vue.prototype.ajax = function(url, method, data, fun, load) {
	let timer = null
	if (load == true || load == undefined) {
		uni.showLoading({
			title: "执行中"
		})
		timer = setTimeout(function() {
			uni.hideLoading()
		}, 60 * 1000)
	}
	uni.request({
		"url": url,
		"method": method,
		"header": {
			token: uni.getStorageSync("token")
		},
		"data": data,
		success: function(resp) {
			if (load == true || load == undefined) {
				clearTimeout(timer)
				uni.hideLoading()
			}
			if (resp.statusCode == 401) {
				uni.removeStorageSync("token")
				uni.showToast({
					icon: "error",
					title: "请登录小程序"
				})
				setTimeout(() => {
					uni.switchTab({
						"url":"/pages/mine/mine"
					})
				}, 2000);
			} else if (resp.statusCode == 200 && resp.data.code == 200) {
				let data = resp.data
				if (data.hasOwnProperty("token")) {
					let token = data.token
					uni.setStorageSync("token", token)
				}
				fun(resp)
			} else {
				uni.showToast({
					icon: "none",
					title: "执行异常"
				})
				console.error(resp.data)
			}
		},
		fail: function(error) {
			if (load == true || load == undefined) {
				clearTimeout(timer)
				uni.hideLoading()
			}
		}
	})
}
