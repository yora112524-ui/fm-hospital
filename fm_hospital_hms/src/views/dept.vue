<template>
  <div v-if="isAuth(['ROOT', 'DEPT:SELECT'])">
    <el-form :inline="true" :model="dataForm" :rules="dataRule" ref="dataForm">
      <el-form-item prop="deptName">
        <el-input
            v-model="dataForm.deptName"
            placeholder="部门名称"
            size="medium"
            class="input"
            clearable="clearable"
        />
      </el-form-item>
      <el-form-item>
        <el-button size="medium" type="primary" @click="searchHandle()">查询</el-button>
        <el-button
            size="medium"
            type="primary"
            @click="addHandle()"
        >
          添加
        </el-button>
        <el-button
            size="medium"
            type="danger"
            @click="deleteHandle()"
        >
          批量删除
        </el-button>
      </el-form-item>
    </el-form>
    <el-table
        :data="dataList"
        border
        v-loading="dataListLoading"
        @selection-change="selectionChangeHandle"
        :cell-style="{ padding: '3px 0' }"
        size="medium"
        style="width: 100%;"
    >
      <el-table-column
          type="selection"
          :selectable="selectable"
          header-align="center"
          align="center"
          width="50"
      />
      <el-table-column type="index" header-align="center" align="center" width="100" label="序号">
        <template #default="scope">
          <span>{{ (pageIndex - 1) * pageSize + scope.$index + 1 }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="deptName" header-align="center" align="center" label="部门名称" min-width="170"/>
      <el-table-column prop="email" header-align="center" align="center" label="邮箱" min-width="270"/>
      <el-table-column header-align="center" align="center" label="员工数量" min-width="120">
        <template #default="scope">
          <span>{{ scope.row.emps }}人</span>
        </template>
      </el-table-column>
      <el-table-column prop="desc" header-align="center" align="center" label="备注" min-width="400"/>
      <el-table-column header-align="center" align="center" width="150" label="操作">
        <template #default="scope">
          <el-button
              type="text"
              size="medium"
              @click="updateHandle(scope.row.id)"
          >
            修改
          </el-button>
          <el-button
              type="text"
              size="medium"
              @click="deleteHandle(scope.row.id)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
        @size-change="sizeChangeHandle"
        @current-change="currentChangeHandle"
        :current-page="pageIndex"
        :page-sizes="[10, 20, 50]"
        :page-size="pageSize"
        :total="totalCount"
        layout="total, sizes, prev, pager, next, jumper"
    ></el-pagination>
    <add-or-update v-if="addOrUpdateVisible" ref="addOrUpdate" @refreshDataList="loadDataList"></add-or-update>
  </div>
</template>

<script>
import AddOrUpdate from './dept-add-or-update.vue';

export default {
  components: {
    AddOrUpdate
  },
  data: function () {
    return {
      dataForm: {
        deptName: null
      },
      dataList: [],
      pageIndex: 1,
      pageSize: 10,
      totalCount: 0,
      dataListLoading: false,
      dataListSelections: [],
      addOrUpdateVisible: false,
      dataRule: {
        deptName: [
          {required: false, pattern: '^[a-zA-Z0-9\u4e00-\u9fa5]{1,10}$', message: '部门名称格式错误'}
        ]
      }
    };
  },
  methods: {
    loadDataList: function () {
      let that = this;
      that.dataListLoading = true;
      let data = {
        deptName: that.dataForm.deptName,
        page: that.pageIndex,
        length: that.pageSize
      };

      that.$http('dept/selectDeptByCondition', 'POST', data, true, function (resp) {
        let page = resp.page;
        // console.log(resp)
        that.dataList = page.list;
        console.log(that.dataList);
        that.totalCount = page.totalCount;
        that.dataListLoading = false;
      });

    },
    searchHandle: function () {
      this.$refs['dataForm'].validate(valid => {
        if (valid) {
          this.$refs['dataForm'].clearValidate();
          if (this.dataForm.deptName == '') {
            this.dataForm.deptName = null;
          }
          if (this.pageIndex != 1) {
            this.pageIndex = 1;
          }
          this.loadDataList();
        } else {
          return false;
        }
      });
    },
    selectable: function (row, index) {
      if (row.emps > 0) {
        return false;
      }
      return true;
    },
    selectionChangeHandle: function (val) {
      this.dataListSelections = val;
    },
    sizeChangeHandle: function (val) {
      this.pageSize = val;
      this.pageIndex = 1;
      this.loadDataList();
    },
    currentChangeHandle: function (val) {
      this.pageIndex = val;
      this.loadDataList();
    },
    deleteHandle: function (id) {
      let that = this;
      let ids = id
          ? [id]
          : that.dataListSelections.map(item => {
            return item.id;
          });
      if (ids.length == 0) {
        that.$message({
          message: '没有选中记录',
          type: 'warning',
          duration: 1200
        });
      } else {
        that.$confirm(`确定要删除选中的记录？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          that.$http('dept/deleteDeptByIds', 'POST', {ids: ids}, true, function (resp) {
            if (resp.rows > 0) {
              that.$message({
                message: '操作成功',
                type: 'success',
                duration: 1200,
                onClose: () => {
                  that.loadDataList();
                }
              });
            } else {
              that.$message({
                message: '未能删除记录',
                type: 'warning',
                duration: 1200
              });
            }
          });
        });
      }
    },
    addHandle: function () {
      this.addOrUpdateVisible = true;
      this.$nextTick(() => {
        this.$refs.addOrUpdate.init();
      });
    },
    updateHandle: function (id) {
      this.addOrUpdateVisible = true;
      this.$nextTick(() => {
        this.$refs.addOrUpdate.init(id);
      });
    }
  },
  created: function () {
    this.loadDataList();
  }
};
</script>

<style lang="less" scoped="scoped"></style>
