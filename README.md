
# Transaction Aggregation API

## Project Overview

The Transaction Aggregation API is a backend application designed to collect, consolidate, normalise and categorise customer financial transactions from multiple mock data sources.

The system integrates different transaction feeds, each representing a financial data provider with its own data structure and format. It processes these transactions into a consistent format, allowing financial information from different sources to be managed and retrieved through a centralised REST API.

The primary objective of the project is to simplify the management of customer financial transactions by providing a unified view of their financial activity. The API also supports retrieving aggregated information, filtering transactions and analysing spending patterns.

As an additional feature, the system includes budget management functionality, allowing customers to establish monthly spending limits and monitor their expenses against their budgets.

## Core Features

- **Transaction Aggregation:** Collects financial transactions from multiple mock data sources and combines them into a centralised system.
- **Transaction Normalisation:** Converts different transaction formats into a consistent transaction structure.
- **Duplicate Detection:** Identifies potential duplicate transactions received from different sources to prevent double-counting.
- **Transaction Categorisation:** Automatically categorises transactions using configurable rules based on transaction descriptions, merchant information, transaction direction and other criteria.
- **Transaction Management:** Provides API endpoints to retrieve, filter, sort and manage customer transactions.
- **Financial Analytics:** Provides aggregated financial information, including income, expenses, spending breakdowns and transaction trends.
- **Data Persistence:** Stores processed transactions in a relational database for retrieval and further analysis.

## Additional Feature: Budget Management

In addition to the core transaction aggregation functionality, the system incorporates a budget management feature to provide further insight into customer spending habits.

Customers can create monthly budgets, establish overall spending limits and define individual limits for specific transaction categories.

The system evaluates transactions against these limits and provides budget-related information, including:

- Monthly budget creation and management.
- Category-specific spending limits.
- Tracking of cumulative spending.
- Remaining budget calculations.
- Budget status indicators.
- Budget health summaries.
- Identification of categories that exceed their allocated limits.

This additional functionality builds on the transaction categorisation feature by using categorised transactions to evaluate spending against the customer's budget.