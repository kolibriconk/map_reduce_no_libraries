import javax.swing.*;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import java.util.List;

public class BarChart extends JFrame {


    public BarChart(String appTitle, List<KeyValuePair<Character, Float>> result_dataset) {
        super(appTitle);

        // Create Dataset
        CategoryDataset dataset = createDataset(result_dataset);

        //Create chart
        JFreeChart chart=ChartFactory.createBarChart(
                "Histogram", //Chart Title
                "Letter", // Category axis
                "Frequency", // Value axis
                dataset,
                PlotOrientation.VERTICAL,
                true,true,false
        );

        ChartPanel panel=new ChartPanel(chart);
        setContentPane(panel);
    }

    private CategoryDataset createDataset(List<KeyValuePair<Character, Float>> result_dataset) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (KeyValuePair<Character, Float> kvp:result_dataset){
            dataset.addValue(kvp.getValue(), kvp.getKey(), "");
        }

        return dataset;
    }
}
