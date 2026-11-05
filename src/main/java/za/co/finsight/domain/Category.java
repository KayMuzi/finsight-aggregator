package za.co.finsight.domain;

import java.util.Arrays;
import java.util.List;

/**
 * Spending and income categories. Only budgetable categories count towards expenses and budgets.
 * INCOME, SAVINGS and TRANSFERS are money movements that are tracked but not budgeted.
 */
public enum Category {
    INCOME(false),
    GROCERIES(true),
    DINING(true),
    TRANSPORT(true),
    UTILITIES(true),
    HOUSING(true),
    INSURANCE(true),
    HEALTHCARE(true),
    ENTERTAINMENT(true),
    SHOPPING(true),
    EDUCATION(true),
    PERSONAL_CARE(true),
    FEES(true),
    OTHER(true),
    SAVINGS(false),
    TRANSFERS(false);

    private final boolean budgetable;

    Category(boolean budgetable) {
        this.budgetable = budgetable;
    }

    public boolean budgetable() {
        return budgetable;
    }

    public static List<Category> budgetableCategories() {
        return Arrays.stream(values()).filter(Category::budgetable).toList();
    }
}
