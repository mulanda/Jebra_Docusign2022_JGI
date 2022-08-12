<template>
  <div class="jgi-media-sync-container">
    <template v-if="isLoading">
      <AppLoader class="jgi-media-sync-loader" loader-message="Loading Months" />
    </template>
    <template v-else>
      <JgiMediaSyncDashboard 
        :dashboard="dashboard"
        :column-maps="columnMaps"
        :months="months"
      />
    </template>
  </div>
</template>

<script>
import AppLoader from '@/components/ui/AppLoader.vue';
import JgiMediaSyncDashboard from '@/modules/dashboards/Jgi/JgiMediaSyncDashboard.vue';
import mediaRecordsColumnMap from '@/modules/dashboards/Jgi/columnMaps/mediaRecordsColumnMap.js';

import jebra from '@/util/jebra';
import { legacyFetchDocs } from '@/util/api';
import { mapActions } from 'vuex';
import { ToastThemes } from '@/util/constants';

export default {

  name: 'JgiMediaSync',

  components: {
    AppLoader,
    JgiMediaSyncDashboard,
  },

  props: {

    dashboard: {
      type: Object,
      required: true
    }

  },

  data() {
    return {
      isLoading: false,
      months: []
    }
  },

  async created() {
    this.jebra = jebra;
    this.columnMaps = {
      'jgi_media_sync:media-records': mediaRecordsColumnMap,
    }
    await this.initializeMonths();
  },

  methods: {

    ...mapActions('toast', [ 'addToast', 'setToastPosition' ]),

    async initializeMonths() {
      const jobId = this.dashboard.jobId;
      try {
        this.isLoading = true;
        const months = await legacyFetchDocs(jobId, 'months');
        this.isLoading = false;        
        this.months = months.map(month => ({ ...month, isSelected: month.isDefault }));
      } catch (error) {
        console.error(error)
        this.addToast({
          theme: ToastThemes.DANGER,
          title: 'Error Fetching Revenue Months',
          subtitle: error,
          expirationTimeout: 4000
        });
        return;
      }
    }

  }
}
</script>

<style lang="scss" scoped>
.jgi-media-sync-container {
  height: 100%;
  width: 100%;
  position: relative;

  & .jgi-media-sync-loader {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
  }
}
</style>