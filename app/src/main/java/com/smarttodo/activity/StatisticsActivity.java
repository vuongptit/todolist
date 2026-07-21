package com.smarttodo.activity;

import android.graphics.Color;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.smarttodo.databinding.ActivityStatisticsBinding;
import com.smarttodo.model.Category;
import com.smarttodo.repository.CategoryRepository;
import com.smarttodo.viewmodel.CategoryViewModel;
import com.smarttodo.viewmodel.StatisticsViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * StatisticsActivity - màn hình thống kê.
 * Hiển thị tổng quan task, PieChart theo category, BarChart theo tháng.
 * Sử dụng MPAndroidChart library.
 */
public class StatisticsActivity extends AppCompatActivity {

    private ActivityStatisticsBinding binding;
    private StatisticsViewModel       statisticsViewModel;
    private CategoryViewModel         categoryViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStatisticsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        statisticsViewModel = new ViewModelProvider(this).get(StatisticsViewModel.class);
        categoryViewModel   = new ViewModelProvider(this).get(CategoryViewModel.class);

        setupToolbar();
        setupCharts();
        setupObservers();

        statisticsViewModel.loadStatistics();
        categoryViewModel.loadCategories();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    /**
     * Cấu hình charts ban đầu
     */
    private void setupCharts() {
        // Pie Chart setup
        binding.pieChart.setHoleColor(Color.parseColor("#252525"));
        binding.pieChart.setHoleRadius(50f);
        binding.pieChart.setTransparentCircleRadius(55f);
        binding.pieChart.getLegend().setTextColor(Color.WHITE);
        binding.pieChart.getDescription().setEnabled(false);
        binding.pieChart.setCenterText("Category");
        binding.pieChart.setCenterTextColor(Color.WHITE);
        binding.pieChart.setCenterTextSize(14f);
        binding.pieChart.setDrawEntryLabels(false);
        binding.pieChart.animateY(1000);

        // Bar Chart setup
        binding.barChart.getDescription().setEnabled(false);
        binding.barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        binding.barChart.getXAxis().setTextColor(Color.parseColor("#AAAAAA"));
        binding.barChart.getAxisLeft().setTextColor(Color.parseColor("#AAAAAA"));
        binding.barChart.getAxisRight().setEnabled(false);
        binding.barChart.getLegend().setTextColor(Color.WHITE);
        binding.barChart.setGridBackgroundColor(Color.TRANSPARENT);
        binding.barChart.animateY(1000);
    }

    private void setupObservers() {
        // Summary stats
        statisticsViewModel.totalTasks.observe(this, total ->
                binding.tvTotalTasks.setText(String.valueOf(total)));

        statisticsViewModel.completedTasks.observe(this, completed ->
                binding.tvCompleted.setText(String.valueOf(completed)));

        statisticsViewModel.pendingTasks.observe(this, pending ->
                binding.tvPending.setText(String.valueOf(pending)));

        statisticsViewModel.completionRate.observe(this, rate ->
                binding.tvCompletionRate.setText(String.format("%.0f%%", rate)));

        // Category chart
        statisticsViewModel.tasksByCategory.observe(this, byCategory -> {
            if (byCategory == null) return;
            // Wait for categories to be loaded
        });

        categoryViewModel.categories.observe(this, categories -> {
            Map<String, Integer> byCategory = statisticsViewModel.tasksByCategory.getValue();
            if (byCategory != null && categories != null) {
                updatePieChart(byCategory, categories);
            }
        });

        // Month chart
        statisticsViewModel.tasksByMonth.observe(this, byMonth -> {
            if (byMonth != null) updateBarChart(byMonth);
        });
    }

    /**
     * Cập nhật Pie Chart theo category
     */
    private void updatePieChart(Map<String, Integer> byCategory, List<Category> categories) {
        List<PieEntry> entries = new ArrayList<>();
        List<Integer>  colors  = new ArrayList<>();

        for (Category cat : categories) {
            Integer count = byCategory.get(cat.getCategoryId());
            if (count != null && count > 0) {
                entries.add(new PieEntry(count, cat.getName()));
                try {
                    colors.add(Color.parseColor(cat.getColor()));
                } catch (Exception e) {
                    colors.add(Color.parseColor("#4CAF50"));
                }
            }
        }

        if (entries.isEmpty()) return;

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);
        dataSet.setSliceSpace(3f);

        PieData pieData = new PieData(dataSet);
        binding.pieChart.setData(pieData);
        binding.pieChart.invalidate();
    }

    /**
     * Cập nhật Bar Chart theo tháng
     */
    private void updateBarChart(Map<String, Integer> byMonth) {
        List<BarEntry> entries = new ArrayList<>();
        List<String>   labels  = new ArrayList<>();

        int index = 0;
        for (Map.Entry<String, Integer> entry : byMonth.entrySet()) {
            entries.add(new BarEntry(index++, entry.getValue()));
            labels.add(entry.getKey());
        }

        if (entries.isEmpty()) return;

        BarDataSet dataSet = new BarDataSet(entries, "Task theo tháng");
        dataSet.setColor(Color.parseColor("#4CAF50"));
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.7f);

        binding.barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        binding.barChart.setData(barData);
        binding.barChart.invalidate();
    }
}
