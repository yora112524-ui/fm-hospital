<template>
	<el-dialog
		title="员工离职"
		v-if="isAuth(['ROOT', 'USER:UPDATE'])"
		:close-on-click-modal="false"
		v-model="visible"
		width="370px"
	>
		<el-form :model="dataForm" ref="dataForm" :rules="dataRule" label-width="80px">
			<el-form-item label="交接人员" prop="assigneeId">
				<el-select v-model="dataForm.assigneeId" size="medium" style="width: 100%;" clearable="clearable">
					<el-option-group v-for="group in userList" :key="group.label" :label="group.label">
						<el-option
							v-for="item in group.options"
							:key="item.value"
							:label="item.label"
							:value="item.value"
						></el-option>
					</el-option-group>
				</el-select>
			</el-form-item>
			<el-form-item class="desc"><span>交接人将接管该离职人员未审批的工作</span></el-form-item>
		</el-form>
		<template #footer>
			<span class="dialog-footer">
				<el-button size="medium" @click="visible = false">取消</el-button>
				<el-button type="primary" size="medium" @click="dataFormSubmit">确定</el-button>
			</span>
		</template>
	</el-dialog>
</template>

<script>
export default {
	data: function() {
		return {
			visible: false,
			dataForm: {
				userId: null,
				assigneeId: null
			},
			userList: [],
			dataRule: {
				assigneeId: [{ required: true, message: '交接人员不能为空' }]
			}
		};
	},
	methods: {
		init: function(id) {
			let that = this;
			that.dataForm.userId = id;
			that.visible = true;
			that.$nextTick(() => {
				that.$refs['dataForm'].resetFields();
				that.$http('user/searchUserGroupByDept', 'POST', {"userId":id}, true, function(resp) {
					let list = resp.list;
					let userList = [];
					for (let one of list) {
						let options = [];
						for (let user of one.users) {
							options.push({ label: user.name, value: user.userId });
						}
						userList.push({
							label: '[  ' + one.deptName + '  ]',
							options: options
						});
					}
					that.userList = userList;
				});
			});
			
		},
		dataFormSubmit: function() {
			let that = this;
			let data = {
				userId: that.dataForm.userId,
				assigneeId: that.dataForm.assigneeId
			};
			this.$refs['dataForm'].validate(valid => {
				if (valid) {
					// console.log(data);
					that.$http('user/dimiss', 'POST', data, true, function(resp) {
						if (resp.rows == 1) {
							that.visible = false;
							that.$message({
								message: '操作成功',
								type: 'success',
								duration: 1200
							});
							that.$emit('refreshDataList');
						} else {
							that.$message({
								message: '操作失败',
								type: 'error',
								duration: 1200
							});
						}
					});
				}
			});
		}
	}
};
</script>

<style lang="less" scoped="scoped">
.desc>div{
	line-height: 1.6 !important;
	font-size: 13px;
	color: #999;
}
</style>
