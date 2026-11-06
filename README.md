
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

````

How to Run and Test the API

The application provides Swagger UI for interacting with and testing the REST API endpoints.
1. Prerequisites
   Before running the application, ensure you have the following installed:
   Java JDK 17 or higher.
   Maven 3.9 or higher.
   MySQL
   Docker and Docker Compose (recommended).


2. Clone the Repository
   Clone the project repository to your local machine:
   git clone https://github.com/KayMuzi/finsight-aggregator.git
   Navigate into the project directory:
   cd finsight-aggregator


3. Configure the API Key
   The application requires an API key to authenticate requests.
   Set the environment variable before starting the application:
   Linux / macOS:
   export FINSIGHT_API_KEY="MySecretApiKey12345"
   Windows PowerShell:
   $env:FINSIGHT_API_KEY="MySecretApiKey12345"
   You can also configure this value in a .env file when using Docker Compose.


4. Start the Application
   Option A: Using Docker Compose (Recommended)
   Run the following command from the project root directory:
   docker compose up --build
   This builds and starts the application and its configured services.
   Option B: Using Maven
   If you prefer running the application locally, ensure MySQL is running and configured.
   Build and test the project:
   mvn verify
   Start the application:
   mvn spring-boot:java
   Once the application has started successfully, it should be accessible at:
   http://localhost:8080


5. Open Swagger UI
   Open your browser and navigate to:
   http://localhost:8080/swagger-ui/index.html#/
   Swagger UI provides an interactive interface where you can view the available API endpoints, inspect request parameters and execute API requests.


6. Authenticate Your Requests
   All API endpoints require an API key, except for the health endpoint, API documentation and Swagger UI.
   Use the following header when making requests:
   X-API-Key: MySecretApiKey12345
   If Swagger UI provides an Authorize option, enter your API key there.


7. Test Transaction Aggregation
   The system provides mock transaction data for two customers:
   CUST-1001
   CUST-1002
   For the initial testing process, use CUST-1001.
   Step 1: Synchronise transactions
   Find the following endpoint:
   POST /api/v1/customers/{customerId}/sync
   Expand the POST endpoint.
   Select Try it out.
   Enter CUST-1001 as the customer ID.
   Select Execute.
   The system will retrieve transactions from the available mock data sources, normalise the records, identify duplicates, categorise transactions and store the processed information.
   Step 2: Retrieve customer transactions
   Find:
   GET /api/v1/customers/{customerId}/transactions
   Select Try it out.
   Enter CUST-1001.
   Select Execute.
   The response should display the customer's aggregated transactions, including their descriptions, amounts, sources and assigned categories.
   Step 3: View available transaction categories
   Find:
   GET /api/v1/categories
   Execute the request to retrieve the available transaction categories and identify which categories support budget limits.
   Step 4: Retrieve a monthly financial summary
   Find:
   GET /api/v1/customers/{customerId}/summary/{month}
   Enter:
   Customer ID: CUST-1001
   Month: 2026-10
   Execute the request to retrieve aggregated financial information, including income, expenses, surplus, savings rate and spending breakdowns.


8. Test the Additional Budget Management Feature
   The application also supports monthly budget creation and spending analysis.
   Step 1: Create a monthly budget
   Find:
   PUT /api/v1/customers/{customerId}/budgets/{month}
   Use the following example request body:

   {
  "totalLimit": 29000,
  "categoryLimits": [
    {
      "category": "GROCERIES",
      "amount": 5000
    },
    {
      "category": "DINING",
      "amount": 1500
    },
    {
      "category": "TRANSPORT",
      "amount": 2000
    },
    {
      "category": "UTILITIES",
      "amount": 1500
    },
    {
      "category": "HEALTHCARE",
      "amount": 3000
    },
    {
      "category": "ENTERTAINMENT",
      "amount": 1000
    },
    {
      "category": "SHOPPING",
      "amount": 1500
    },
    {
      "category": "PERSONAL_CARE",
      "amount": 800
    },
    {
      "category": "FEES",
      "amount": 200
    },
    {
      "category": "HOUSING",
      "amount": 9500
    }
  ]
}




   Enter the customer ID and month, then execute the request.
   Step 2: Retrieve budget information
   Find:
   GET /api/v1/customers/{customerId}/budgets/{month}
   This retrieves the budget configured for the selected month.
   Step 3: Check budget health
   Find:
   GET /api/v1/customers/{customerId}/budgets/{month}/health
   The response provides information about:
   Total budget limit.
   Total spending.
   Remaining budget.
   Percentage of budget used.
   Spending per category.
   Budget health status.
   Step 4: Retrieve transactions exceeding the budget
   Find:
   GET /api/v1/customers/{customerId}/transactions
   Add the following query parameter:
   budgetStatus=OVER_BUDGET
   Execute the request to retrieve transactions that have exceeded their applicable category budget.


9. Explore Additional API Endpoints
   Other available endpoints include:
   Method	Endpoint	Purpose
   GET	/transactions	Retrieve and filter transactions
   GET	/transactions/{transactionId}	Retrieve a specific transaction
   PATCH	/transactions/{transactionId}/category	Override a transaction category
   GET	/syncs	Retrieve previous synchronisation runs
   GET	/budgets	Retrieve customer budgets
   DELETE	/budgets/{month}	Delete a monthly budget
   GET	/analytics/categories/{month}	Retrieve category spending breakdown
   GET	/analytics/merchants/{month}	Retrieve merchant spending rankings
   GET	/analytics/daily/{month}	Retrieve daily spending information
   GET	/analytics/trend	Retrieve financial trends across multiple months
   All endpoints are available through Swagger UI and require the appropriate customer ID, parameters and API key.


10. Run the Tests
    The project includes automated unit and integration tests.
    Run the complete test suite:
    mvn test
    Run individual test classes:
    
    mvn -Dtest=CategorisationEngineTest test
    mvn -Dtest=BudgetEvaluatorTest test
    mvn -Dtest=SyncServiceTest test
    
    These tests cover transaction categorisation, budget calculations, synchronisation failures, API authentication, duplicate detection and other application functionality.

11. Verify Application Health
    To check whether the application is running, navigate to:
    http://localhost:8080/actuator/health
    A successful response indicates that the application is running.
    API Documentation
    The API documentation is available through Swagger UI:
    Open Swagger UI – Transaction Aggregation API
    Swagger UI allows developers to explore available endpoints, view request and response structures, and execute API requests interactively.
