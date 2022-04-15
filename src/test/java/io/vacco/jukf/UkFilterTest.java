package io.vacco.jukf;

import com.google.gson.GsonBuilder;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static j8spec.J8Spec.it;
import static io.vacco.sabnock.SkJson.*;

@RunWith(J8SpecRunner.class)
public class UkFilterTest {
  static {
    it("Computes approximate values on a sine signal", () -> {
      var filter = new UkFilter(0, UkFilterParams.getDefault());

      List<Double> measurements = new ArrayList<>();
      List<Double> states = new ArrayList<>();
      Random rnd = new Random();

      for (int k = 0; k < 100; k++) {
        measurements.add(Math.sin(k * 3.14 * 5 / 180) + (double) rnd.nextInt(50) / 100);
      }

      for (Double measurement : measurements) {
        filter.update(new double[] {measurement});
        states.add(filter.getState()[0]);
      }

      var g = new GsonBuilder().setPrettyPrinting().create();
      var chart = obj(
          kv("tooltip", obj(kv("trigger", "axis"))),
          kv("grid", obj(kv("containLabel", "true"))),
          kv("xAxis", obj(
              kv("type", "category"),
              kv("data", IntStream.range(0, measurements.size()).boxed().collect(Collectors.toList()))
          )),
          kv("yAxis", obj(kv("type", "value"))),
          kv("dataZoom", new Object[] { obj(kv("type", "inside")), obj() }),
          kv("series", new Object[] {
              obj(
                  kv("name", "measurement"),
                  kv("type", "line"),
                  kv("data", measurements)
              ),
              obj(
                  kv("name", "KfEstimate"),
                  kv("type", "line"),
                  kv("data", states)
              )
          })
      );

      for (double[] cvr : filter.getCovariance()) {
        System.out.println(Arrays.toString(cvr));
      }

      System.out.println(g.toJson(chart));
    });
  }
}
