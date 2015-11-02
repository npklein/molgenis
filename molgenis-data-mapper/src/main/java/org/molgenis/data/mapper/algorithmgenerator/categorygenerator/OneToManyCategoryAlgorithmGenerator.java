package org.molgenis.data.mapper.algorithmgenerator.categorygenerator;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jscience.physics.amount.Amount;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.DataService;
import org.molgenis.data.mapper.algorithmgenerator.bean.AmountWrapper;
import org.molgenis.data.mapper.algorithmgenerator.bean.Category;

public class OneToManyCategoryAlgorithmGenerator extends CategoryAlgorithmGenerator
{
	private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("##.#",
			DecimalFormatSymbols.getInstance(new Locale("en")));

	private final OneToOneCategoryAlgorithmGenerator oneToOneCategoryAlgorithmGenerator;

	public OneToManyCategoryAlgorithmGenerator(DataService dataService)
	{
		super(dataService);
		oneToOneCategoryAlgorithmGenerator = new OneToOneCategoryAlgorithmGenerator(dataService);
	}

	@Override
	public boolean isSuitable(AttributeMetaData targetAttribute, List<AttributeMetaData> sourceAttributes)
	{
		return sourceAttributes.size() > 1;
	}

	@Override
	public String generate(AttributeMetaData targetAttribute, List<AttributeMetaData> sourceAttributes)
	{
		// if the target attribute and all the source attributes contain frequency related categories
		StringBuilder stringBuilder = new StringBuilder();

		if (suitableForGeneratingWeightedMap(targetAttribute, sourceAttributes))
		{
			for (AttributeMetaData sourceAttribute : sourceAttributes)
			{
				String generateWeightedMap = generateWeightedMap(sourceAttribute);

				if (StringUtils.isNotEmpty(generateWeightedMap))
				{
					if (stringBuilder.length() == 0)
					{
						stringBuilder.append("var SUM_WEIGHT = new newValue(0);\n");
					}

					stringBuilder.append("SUM_WEIGHT.plus(").append(generateWeightedMap).append(");\n");
				}
			}
			stringBuilder.append("SUM_WEIGHT").append(groupCategoryValues(targetAttribute));
		}
		else
		{
			for (AttributeMetaData sourceAttribute : sourceAttributes)
			{
				stringBuilder.append(
						oneToOneCategoryAlgorithmGenerator.generate(targetAttribute, Arrays.asList(sourceAttribute)));
			}
		}

		return stringBuilder.toString();
	}

	boolean suitableForGeneratingWeightedMap(AttributeMetaData targetAttribute,
			List<AttributeMetaData> sourceAttributes)
	{
		boolean isTargetSuitable = oneToOneCategoryAlgorithmGenerator
				.isFrequencyCategory(convertToCategory(targetAttribute));
		boolean areSourcesSuitable = sourceAttributes.stream().map(this::convertToCategory)
				.allMatch(oneToOneCategoryAlgorithmGenerator::isFrequencyCategory);
		return isTargetSuitable && areSourcesSuitable;
	}

	public String generateWeightedMap(AttributeMetaData attributeMetaData)
	{
		StringBuilder stringBuilder = new StringBuilder();

		for (Category sourceCategory : convertToCategory(attributeMetaData))
		{
			AmountWrapper amountWrapper = sourceCategory.getAmountWrapper();
			if (amountWrapper != null)
			{
				if (stringBuilder.length() == 0)
				{
					stringBuilder.append("$('").append(attributeMetaData.getName()).append("').map({");
				}

				double estimatedValue = amountWrapper.getAmount().getEstimatedValue();

				stringBuilder.append("\"").append(sourceCategory.getCode()).append("\":")
						.append(DECIMAL_FORMAT.format(estimatedValue)).append(",");
			}
		}

		if (stringBuilder.length() > 0)
		{
			stringBuilder.deleteCharAt(stringBuilder.length() - 1);
			stringBuilder.append("}, null, null).value()");
		}

		return stringBuilder.toString();
	}

	public String groupCategoryValues(AttributeMetaData attributeMetaData)
	{
		StringBuilder stringBuilder = new StringBuilder();

		List<Category> sortedCategories = convertToCategory(attributeMetaData).stream()
				.filter(category -> category.getAmountWrapper() != null).collect(Collectors.toList());

		Collections.sort(sortedCategories, new Comparator<Category>()
		{
			public int compare(Category o1, Category o2)
			{
				return Double.compare(o1.getAmountWrapper().getAmount().getEstimatedValue(),
						o2.getAmountWrapper().getAmount().getEstimatedValue());
			}
		});

		List<Integer> sortedRangValues = getRangedValues(sortedCategories);

		Map<String, Category> categoryRangeBoundMap = createCategoryRangeBoundMap(sortedCategories);

		if (categoryRangeBoundMap.size() > 0)
		{
			if (stringBuilder.length() == 0)
			{
				stringBuilder.append(".group([");
			}

			sortedRangValues.stream().forEach(value -> stringBuilder.append(value).append(','));
			stringBuilder.deleteCharAt(stringBuilder.length() - 1);
			stringBuilder.append("]).map({");

			for (Entry<String, Category> entry : categoryRangeBoundMap.entrySet())
			{
				stringBuilder.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue().getCode())
						.append("\",");
			}
			stringBuilder.deleteCharAt(stringBuilder.length() - 1);
			stringBuilder.append("}, null, null).value();");
		}

		return stringBuilder.toString();
	}

	private List<Integer> getRangedValues(List<Category> sortedCategories)
	{
		List<Integer> sortedRangValues = new ArrayList<>();
		for (Category targetCategory : sortedCategories)
		{
			Amount<?> amount = targetCategory.getAmountWrapper().getAmount();
			int minValue = Integer.parseInt(DECIMAL_FORMAT.format(amount.getMinimumValue()));
			int maxValue = Integer.parseInt(DECIMAL_FORMAT.format(amount.getMaximumValue()));

			if (!sortedRangValues.contains(minValue))
			{
				sortedRangValues.add(minValue);
			}

			if (!sortedRangValues.contains(maxValue))
			{
				sortedRangValues.add(maxValue);
			}
		}
		return sortedRangValues;
	}

	private Map<String, Category> createCategoryRangeBoundMap(List<Category> sortedCategories)
	{
		Map<String, Category> categoryRangeBoundMap = new LinkedHashMap<>();

		for (int categoryIndex = 0; categoryIndex < sortedCategories.size(); categoryIndex++)
		{
			Category category = sortedCategories.get(categoryIndex);
			Amount<?> amount = category.getAmountWrapper().getAmount();
			int minValue = Integer.parseInt(DECIMAL_FORMAT.format(amount.getMinimumValue()));
			int maxValue = Integer.parseInt(DECIMAL_FORMAT.format(amount.getMaximumValue()));

			if (categoryIndex == 0)
			{
				categoryRangeBoundMap.put("-" + minValue, category);
			}

			// The category contains ranged values
			if (minValue != maxValue)
			{
				categoryRangeBoundMap.put(minValue + "-" + maxValue, category);
			}
			else
			{
				if (categoryIndex < sortedCategories.size() - 1)
				{
					Category nextCategory = sortedCategories.get(categoryIndex + 1);
					int upperBound = Integer.parseInt(
							DECIMAL_FORMAT.format(nextCategory.getAmountWrapper().getAmount().getMinimumValue()));
					if (upperBound != maxValue)
					{
						categoryRangeBoundMap.put(maxValue + "-" + upperBound, category);
					}
				}

				if (categoryIndex > 0)
				{
					Category previousCategory = sortedCategories.get(categoryIndex - 1);
					int lowerBound = Integer.parseInt(
							DECIMAL_FORMAT.format(previousCategory.getAmountWrapper().getAmount().getMaximumValue()));
					if (lowerBound != maxValue)
					{
						categoryRangeBoundMap.put(lowerBound + "-" + maxValue, category);
					}
				}
			}
			if (categoryIndex == sortedCategories.size() - 1)
			{
				categoryRangeBoundMap.put(maxValue + "+", category);
			}
		}
		return categoryRangeBoundMap;
	}
}
