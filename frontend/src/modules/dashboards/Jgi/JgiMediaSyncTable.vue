<template>
  <div class="jb-table-container">
    <table class="jb-table">
      <thead class="jb-thead">
        <tr :class="['jb-trow', theme]">
          <th v-for="column in tableColumnMapOrderedKeys" :key="column" class="jb-tcell">
            <div class="jb-tcell-content">{{ tableColumnMap[column].label }}</div>
          </th>
        </tr>
      </thead>      
      <tbody class="jb-tbody">
        <tr v-for="row in rowsDataToDisplay" :key="row[rowIdentifier]" :class="['jb-trow', theme]">
          <td 
            v-for="column in tableColumnMapOrderedKeys" 
            :key="column" 
            :class="['jb-tcell', column]"
          >
            <div class="jb-tcell-content">
              <template v-if="column === 'Date'">
                {{ formatDate(row[column]) }}
              </template>
              <template v-else-if="column === 'LineAmount'">
                {{ formatLineAmount(row[column]) }}
              </template>
              <template v-else>
                {{ row[column] }}
              </template>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
import dashTable from '@/mixin/datadashboard/mDashTable.js';

import { parse, format } from 'date-fns';

export default {

  name: 'JgiMediaSyncTable',

  mixins: [dashTable],

  methods: {

    formatDate(date) {
      if (!date) return;
      const parsedDate = parse(date, 'yyyy-MM-dd', new Date());
      const formattedDate = format(parsedDate, 'EEE, d MMM, yyyy');
      return formattedDate;
    },

    formatLineAmount(amount) {
      if (!amount) return;
      const isAmountNegative = amount < 0;
      const amountAbsolute = Math.abs(amount);
      const roundedAmount = this.round(amountAbsolute, 2);
      const roundedAmountString = roundedAmount.toFixed(2);
      const roundedAmountWithCommas = this.numberWithCommas(roundedAmountString);
      return isAmountNegative  ? `-$${roundedAmountWithCommas}` : `$${roundedAmountWithCommas}`;
    },

    round(num, places) {
      return +(Math.round(num + 'e+' + places)  + 'e-' + places);
    },

    numberWithCommas(num) {
      var parts = num.toString().split('.');
      parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
      return parts.join('.');
    }

  }

}
</script>

<style lang="scss" scoped>
@import "@/assets/css/jbTable.scss";
</style>