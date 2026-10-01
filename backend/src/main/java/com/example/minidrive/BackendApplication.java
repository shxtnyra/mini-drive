package com.example.minidrive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication(exclude = { UserDetailsServiceAutoConfiguration.class })
@EnableAsync
@ConfigurationPropertiesScan("com.example.minidrive.configuration.properties")
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);

//		List<Integer> array = List.of(5,3,221,99,7,2,1,15,26,41,2);
//		int startIndex = 0;
//		int endIndex = array.size() - 1;
//
//		System.out.println(endIndex);
//		System.out.println(array);
//		System.out.println(mergeSort(array,startIndex, endIndex));


	}

	public static List<Integer> mergeSort(List<Integer> array, int start, int end) {
		if (start == end) {
			return List.of(array.get(start));
		}

		if (start > end) {
			return List.of();
		}

		int centerIndex = (end + start) / 2;
		List<Integer> leftPart = mergeSort(array, start, centerIndex);
		List<Integer> rightPart = mergeSort(array, centerIndex + 1, end);
        return merge(leftPart, rightPart);
	}

	public static List<Integer> merge(List<Integer> ar1, List<Integer> ar2) {
		List<Integer> result = new ArrayList<>();

		int i = 0, j = 0;

		while (i < ar1.size() &&  j < ar2.size()) {
			if (ar1.get(i) < ar2.get(j))
				result.add(ar1.get(i++));
			else
				result.add(ar2.get(j++));
		}

		while (i < ar1.size()) result.add(ar1.get(i++));
		while (j < ar2.size()) result.add(ar2.get(j++));

		return result;
	}



}
