<template>
  <div class="jb-dashboard-container">
    <div class="jb-dashboard">
      <div class="jb-dashboard-title-bar">
        <div class="title-left">
          <div class="title">{{ dashboard.title }}</div>
          <AppMultiActionDropdown
            :options="availableMonths"
            unique-key="id"
            :disabled="!canSetupDashboardData"
            @option-selected="changeSelectedMonth"
          />
        </div>
        <div class="title-right">
          <AppButton
            class="update-btn"
            text="Update"
            theme="secondary"
            :disabled="!canSetupDashboardData"
            @on-click="onUpdateJobData"
          />
          <AppButton
            class="download-btn"
            text="Download Zip"
            theme="secondary"
            :disabled="!canSetupDashboardData"
            @on-click="downloadZip"
          />
          <AppButton
            class="sign-btn"
            text="Sign Zip"
            theme="blue"
            :disabled="!canSetupDashboardData"
            @on-click="signZip"
          />
        </div>
      </div>

      <JgiMediaSyncFilterToolbar
        v-if="isMediaRecordsDashboard"
        :is-media-record-dashboard="isMediaRecordsDashboard"
        :description-list="createDataList('Description')"
        :account-code-list="createDataList('AccountCode')"
        :account-name-list="createDataList('AccountName')"
        @apply-client-filter="applyClientFilter"
        @reset-filters="resetFilters"
      />
      <JgiMediaSyncFilterToolbar
        v-else-if="isAccountsDashboard"
        :is-accounts-dashboard="isAccountsDashboard"
        :type-list="createDataList('Type')"
        :tax-type-list="createDataList('Tax_Type')"
        @apply-client-filter="applyClientFilter"
        @reset-filters="resetFilters"
      />

      <div v-if="isLoading" class="dashboard-data-info">
        <AppLoader :loader-message="loaderText" />
      </div>
      <div v-else-if="dashboardDocsNotFound" class="dashboard-data-info">
        <div>
          No  <span class="text-highlight">{{ dashboard.title }}</span> data
        </div>
      </div>
      <template v-else>
        <JgiMediaSyncTable
          class="job-data-table"
          theme="THEME_PRIMARY"
          :column-map="columnMap"
          :rows-data="tableData"
          ref="jobDataTable"
        />
        <div class="pagination">
          <span class="page-size-label">Showing</span>
          <select v-model="pageSize" class="page-size-select">
            <option :value="10">10</option>
            <option :value="50">50</option>
            <option :value="100">100</option>
            <option :value="rootDataSize">{{ rootDataSize }}</option>
          </select>
          <span>rows from page number </span>
          <select v-model="pageNumber" class="page-number-select">
            <option
              v-for="pageNumberOption in pageNumberOptions"
              :value="pageNumberOption"
              :key="pageNumberOption"
            >
              {{ pageNumberOption + 1 }}
            </option>
          </select>
        </div>
      </template>
    </div>
  </div>
</template>

<script>
import AppMultiActionDropdown from '@/components/ui/AppMultiActionDropdown.vue';
import AppButton from '@/components/ui/AppButton.vue';
import AppLoader from '@/components/ui/AppLoader.vue';
import JgiMediaSyncFilterToolbar from '@/modules/dashboards/Jgi/JgiMediaSyncFilterToolbar.vue';
import JgiMediaSyncTable from '@/modules/dashboards/Jgi/JgiMediaSyncTable.vue';

import dashDataSetup from '@/mixin/datadashboard/mDashDataSetup.js';
import dashActions from '@/mixin/datadashboard/mDashActions.js';
import { parse, format, startOfMonth, endOfMonth } from 'date-fns';
import { downloadDataDocumentNew, updateJobData, postJobDataAction } from '@/util/api';
import download from 'downloadjs';
import { mapActions } from 'vuex';
import { ToastThemes } from '@/util/constants';
import jebra from '@/util/jebra';

export default {

  name: 'JgiMediaSyncDashboard',

  components: {
    AppMultiActionDropdown,
    AppButton,
    AppLoader,
    JgiMediaSyncFilterToolbar,
    JgiMediaSyncTable
  },

  mixins: [dashDataSetup, dashActions],

  props: {

    useFiltersCache: {
      type: Boolean,
      default: false,
    },

    selectableRows: {
      type: Boolean,
      default: false
    },

    months: {
      type: Array,
      default: () => []
    }
    
  },

  watch: {

    dashboard() {
      this.reset();
    }

  },

  data() {
    return {
      monthsData: this.months, 

      applyClientFilterFlag: 0,
      clientFilters: {},

      pageNumber: 0,
      pageSize: 50,

      resetFlag: 0,
    }
  },

  computed: {

    isMediaRecordsDashboard() {
      //console.error(this.dashboard);
      return this.dashboard.name === 'media-records';
    },

    isAccountsDashboard() {
      return this.dashboard.name === 'accounts';
    },

    rootData() {
      if (!this.dashboardDocs) return null;

      return this.dashboardDocs.map(doc => ({
        ...doc,
        DateTime: parse(doc.Date, 'yyyy-MM-dd', new Date())
      }))
    },

    filteredData() {
      this.resetFlag;
      this.applyClientFilterFlag;

      console.warn(`filtered...`);

      if (!this.rootData) return null;

      console.warn(`filtered...2`);

      if (this.isMediaRecordsDashboard) {
        const dateRangeFilter = this.getFilter('dateRange', this.selectedMonth.dateRange);
        //const descriptionFilter = this.getFilter('Description', this.createDataList('Description'));
        //const accountCodeFilter = this.getFilter('AccountCode', this.createDataList('AccountCode'));
        //const accountNameFilter = this.getFilter('AccountName', this.createDataList('AccountName'));

        //console.error(dateRangeFilter);

        return this.rootData.filter(row => {
          /** @todo: Fix columns, date range filter */
          //console.warn(row);
          return (
            true
            //row.cameraDay >= dateRangeFilter[0] &&
            //row.cameraDay <= dateRangeFilter[1]
            //descriptionFilter.includes(row.Description) &&
            //accountCodeFilter.includes(row.AccountCode) &&
            //accountNameFilter.includes(row.AccountName)
          );
        });
      } else if (this.isAccountsDashboard) {
        const dateRangeFilter = this.getFilter('dateRange', this.selectedMonth.dateRange);
        const typeFilter = this.getFilter('Type', this.createDataList('Type'));
        const taxTypeFilter = this.getFilter('Tax_Type', this.createDataList('Tax_Type'));
        
        return this.rootData.filter(row => {
          return (
            row.DateTime >= dateRangeFilter[0] &&
            row.DateTime <= dateRangeFilter[1] &&
            typeFilter.includes(row.Type) &&
            taxTypeFilter.includes(row.Tax_Type)
          );
        });
      } else {
        return [];
      }
    },

    tableData() {
      const pageStart = this.pageNumber * this.pageSize;
      const nextPageStart = (this.pageNumber + 1) * this.pageSize;
      return this.filteredData ? this.filteredData.slice(pageStart, nextPageStart) : null;
    },

    pageNumberOptions() {
      const maxPages = Math.floor(this.filteredData.length / this.pageSize) +
                       Math.ceil((this.filteredData.length % this.pageSize) / this.pageSize);
      return [...Array(maxPages).keys()];
    },

    availableMonths() {
      return this.monthsData.map(month => this.reformatMonth(month));
    },

    selectedMonth() {
      return this.availableMonths.find(month => month.isSelected);
    },

    dashboardApiDataPath() {
      if (this.dashboardName === 'media-records') {
        return `months/${this.selectedMonth.id}/media-records`;
      } else if (this.dashboardName === 'accounts') {
        return `months/${this.selectedMonth.id}/accounts`;
      }
      return '';
    },

    canSetupDashboardData() {
      return this.availableMonths.length > 0;
    }
    
  },

  methods: {

    ...mapActions('toast', [ 'addToast', 'setToastPosition' ]),

    async fetchDataInStore(filters = []) {
      this.loadingDashboardDocs = true;
      await this.fetchDashboardDocs({
        jobId: this.jobId,
        path: this.dashboardApiDataPath,
        dashboardName: this.dashboardName,
      });
      this.loadingDashboardDocs = false;
    },

    reset() {
      this.resetFilters();
    },

    resetFilters() {
      this.clientFilters = {};
      this.applyClientFilterFlag = 0;
      this.resetFlag = this.resetFlag + 1;
      this.pageNumber = 0;
      this.pageSize = 50;
    },

    // async applyApiFilter({ type, value }) {
    //   if (type === 'DATE_RANGE') {
    //     await this.reloadDashboardDocsInStore([{ type: 'dateRange', value }]);
    //   }
    // },

    applyClientFilter({ type, value }) {
      this.clientFilters[type] = value;
      this.applyClientFilterFlag = this.applyClientFilterFlag + 1;

      // Reset pageNumber to 0 since the filtered results may not be displayed on the current page
      this.pageNumber = 0;
    },

    async downloadZip() {
      try {
        const result = await downloadDataDocumentNew(this.jobId, `months/${this.selectedMonth.id}`);
        download(result, `Jebra_JgiMediaSync_${this.selectedMonth.id}.zip`);
      } catch (error) {
        console.error(error)
        this.addToast({
          theme: ToastThemes.DANGER,
          title: 'Error Downloading Zip',
          subtitle: error,
          expirationTimeout: 4000
        });
        return;
      }
    },

    async onUpdateJobData() {
      try {
        this.addToast({
          theme: ToastThemes.PRIMARY,
          title: 'Triggering Job Data Update',
          subtitle: 'This action can take several minutes',
          expirationTimeout: 4000
        });
        await updateJobData(this.jobId, this.dashboardApiDataPath);
        this.addToast({
          theme: ToastThemes.SUCCESS,
          title: 'Triggered Job Data Update',
          subtitle: 'This action can take several minutes',
          expirationTimeout: 4000
        });
      } catch (error) {
        console.error(error)
        this.addToast({
          theme: ToastThemes.DANGER,
          title: 'Error Performing Action',
          subtitle: error,
          expirationTimeout: 4000
        });
        return;
      }
    },

    async signZip() {
      try {
        this.isLoading = true;
        const api = this.api;
        const path = this.dashboardApiDataPath;
        console.error(path);
        const result = await postJobDataAction(
          this.jobId, 
          `${this.dashboardApiDataPath}/sign`,
          {
            callback: `${window.location.href}?v=success`,
          }
        );
        console.error(result);
        // this.isLoading = false;
        window.location.href = result.url;
      } catch (ex) {
        this.isLoading = false;
        console.error(ex);
        this.addToast({
          theme: ToastThemes.DANGER,
          title: 'Error Signing Zip',
          subtitle: ex.getMessage(),
          expirationTimeout: 5000
        });
      }
    },

    getFilter(key, defaultList) {
      if (key === 'dateRange') {
        const cfDateRange = this.clientFilters.dateRange;
        const filter = (cfDateRange && cfDateRange[0] && cfDateRange[1]) ? cfDateRange : defaultList;
        return filter.map(date => parse(date, 'yyyy-MM-dd', new Date()))
      }

      return this.clientFilters[key] ?? defaultList;
    },

    createDataList(key) {
      if (!this.rootData) return [];
      return [...new Set(this.rootData.map(row => row[key]))];
    },

    reformatMonth(month) {
      const monthDateTime = parse(month.id, 'yyyy-MM', new Date());

      return {
        ...month,
        monthDateTime,
        displayValue: format(monthDateTime, 'MMMM yyyy'),
        dateRange: [
          format(startOfMonth(monthDateTime), 'yyyy-MM-dd'),
          format(endOfMonth(monthDateTime), 'yyyy-MM-dd'),
        ]
      }
    },

    async changeSelectedMonth(newMonthId) {
      this.monthsData = this.monthsData.map(month => ({...month, isSelected: month.id === newMonthId}));
      await this.reloadDashboardDocsInStore();
      // await this.applyApiFilter({
      //   type: 'DATE_RANGE',
      //   value: this.selectedMonth.dateRange
      // });
    },

  }

}
</script>

<style lang="scss" scoped>
.jb-dashboard {

  & .jb-dashboard-title-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;

    & .title-left {
      display: flex;
      align-items: left;
    }

    & .title-right {
      display: flex;
      align-items: right;
    }

    & .title {
      font-size: 24px;
      font-weight: bold;
      margin-right: 24px;
    }

    & .update-btn {
      margin-right: 16px;
    }
  }

  & .dashboard-data-info {
    margin: 320px;
    display: flex;
    justify-content: center;

    & .text-highlight {
      color: var(--primaryColor);
    }
  }

  // & .job-data-table {
  // }

  & .pagination {
    margin-top: 24px;

    & .page-size-select {
      width: 64px;
      margin: 0 8px;
    }

    & .page-number-select {
      width: 64px;
      margin: 0 8px;
    }
  }
}

</style>