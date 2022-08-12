<template>
  <div class="z2x-filter-toolbar-container">
    <div class="toolbar-header">
      <div class="toolbar-title">Apply Filters</div>
      <div class="toolbar-controls">
        <AppButton
          text="Reset Filters" 
          :theme="'primary'"
          :aria-disabled="true"
          @on-click="onResetFilters"
        />
      </div>
    </div>
    <div class="filters-container">
      <template v-if="isMediaRecordDashboard">
        <DatePicker 
          class="filter daterange-filter"
          range
          v-model:value="dateRange"
          :clearable="true"
          format="YYYY-MM-DD"
          value-type="format"
          separator=" to "
          placeholder="Select date range"
          @change="onDateRangeValueChanged"
        />

        <AppSelectSearchDropdown 
          class="filter description-filter"
          :options="descriptionList" 
          :placeholder="getDropdownPlaceholder('selectedDescriptions', 'Descriptions')" 
          :reset-flag="resetFlag"
          @selected-options-change="onDropdownSelectionChange('Description', 'selectedDescriptions', $event)"
        />

        <AppSelectSearchDropdown 
          class="filter account-code-filter"
          :options="accountCodeList" 
          :placeholder="getDropdownPlaceholder('selectedAccountCodes', 'Account Codes')" 
          :reset-flag="resetFlag"
          @selected-options-change="onDropdownSelectionChange('AccountCode', 'selectedAccountCodes', $event)"
        />

        <AppSelectSearchDropdown 
          class="filter account-name-filter"
          :options="accountNameList" 
          :placeholder="getDropdownPlaceholder('selectedAccountNames', 'Account Names')" 
          :reset-flag="resetFlag"
          @selected-options-change="onDropdownSelectionChange('AccountName', 'selectedAccountNames', $event)"
        />
      </template>
      <template v-else-if="isAccountsDashboard">
        <DatePicker 
          class="filter daterange-filter"
          range
          v-model:value="dateRange"
          :clearable="true"
          format="YYYY-MM-DD"
          value-type="format"
          separator=" to "
          placeholder="Select date range"
          @change="onDateRangeValueChanged"
        />

        <AppSelectSearchDropdown 
          class="filter type-filter"
          :options="typeList" 
          :placeholder="getDropdownPlaceholder('selectedTypes', 'Types')" 
          :reset-flag="resetFlag"
          @selected-options-change="onDropdownSelectionChange('Type', 'selectedTypes', $event)"
        />

        <AppSelectSearchDropdown 
          class="filter tax-type-filter"
          :options="taxTypeList" 
          :placeholder="getDropdownPlaceholder('selectedTaxTypes', 'Tax Types')" 
          :reset-flag="resetFlag"
          @selected-options-change="onDropdownSelectionChange('Tax_Type', 'selectedTaxTypes', $event)"
        />
      </template>
    </div>
  </div>
</template>

<script>
import AppSelectSearchDropdown from '@/components/ui/AppSelectSearchDropdown.vue';
import AppButton from '@/components/ui/AppButton.vue';
import DatePicker from 'vue-datepicker-next';

export default {

  name: 'JgiMediaSyncFilterToolbar',

  components: {
    AppSelectSearchDropdown,
    AppButton,
    DatePicker
  },

  props: {

    isMediaRecordDashboard: {
      type: Boolean,
      default: false
    },

    isAccountsDashboard: {
      type: Boolean,
      default: false
    },

    descriptionList: {
      type: Array,
      default: () => []
    },

    accountCodeList: {
      type: Array,
      default: () => []
    },

    accountNameList: {
      type: Array,
      default: () => []
    },

    typeList: {
      type: Array,
      default: () => []
    },

    taxTypeList: {
      type: Array,
      default: () => []
    },

  },

  emits: ['resetFilters', 'applyClientFilter'],

  data() {
    return {
      dateRange: null,

      // For MediaRecord
      selectedDescriptions: null,
      selectedAccountCodes: null,
      selectedAccountNames: null,
      
      // For Accounts
      selectedTypes: null,
      selectedTaxTypes: null,

      resetFlag: 0,
    }
  },

  methods: {

    onResetFilters() {
      this.dateRange = null;
      this.resetFlag = this.resetFlag + 1;
      
      this.$emit('resetFilters');
    },

    onDateRangeValueChanged() {
      if (this.dateRange) {
        this.$emit('applyClientFilter', {
          type: 'dateRange',
          value: this.dateRange
        });
      }
    },

    onDropdownSelectionChange(columnKey, dataKey, selectedValues) {
      this[dataKey] = selectedValues.length > 0 ? selectedValues : null;
      this.$emit('applyClientFilter', {
        type: columnKey,
        value: this[dataKey]
      });
    },

    getDropdownPlaceholder(dataKey, defaultValue) {
      return this[dataKey] ? this[dataKey].join(', ') : defaultValue;
    },

  }
}
</script>

<style lang="scss" scoped>
.z2x-filter-toolbar-container {
  border: 1px solid black;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 24px;

  & .toolbar-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
  }

  & .toolbar-title {
    font-size: 18px;
    font-weight: bold;
  }

  & .toolbar-controls {
    display: flex;
  }

  & .page-type-toggle {
    display: flex;
    align-items: center;
    margin-right: 24px;
    border: 1px solid black;
    border-radius: 16px;
    padding: 4px 16px;
  }

  & .toolbar-reset-btn {
    padding: 4px 16px;
    border-radius: 16px;
    color: var(--whiteColor);
    background-color: #6e40a373;
    text-align: center;
    line-height: 24px;
    
    &:hover {
      background-color: var(--primaryColor);
      cursor: pointer;
    }
  }

  & .filters-container {
    display: flex;
    flex-wrap: wrap;
    // justify-content: space-between;

    & .filter {
      margin-right: 16px;
      margin-bottom: 16px;

      &:last-of-type {
        margin-right: 0;
      }
    }
  }

  & .daterange-filter {
    flex-basis: 210px;
    max-width: 300px;
    flex-grow: 1;
    border: 2px solid var(--primaryColor);
    border-radius: 8px; 
  }

  & .description-filter {
    flex-basis: 180px;
    flex-grow: 1;
    max-width: 260px;
  }

  & .account-code-filter {
    flex-basis: 180px;
    flex-grow: 1;
    max-width: 260px;
  }

  & .account-name-filter {
    flex-basis: 210px;
    flex-grow: 1;
    max-width: 300px;
  }

  & .type-filter {
    flex-basis: 180px;
    flex-grow: 1;
    max-width: 260px;
  }

  & .tax-type-filter {
    flex-basis: 180px;
    flex-grow: 1;
    max-width: 260px;
  }
}
</style>