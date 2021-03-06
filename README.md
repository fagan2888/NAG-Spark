# NAG Spark

These are a collection of examples for using [the NAG Library] on Apache Spark. Current NAG-Spark implemetations are in Java and include the following functionality:
 - Simple Statistical Calculations (mean, sd, skewness, kurtosis, min, max)
 - Pearson Correlation 
 - Principal Component Analysis
 - Linear Regression - exact coefficients with one pass over the data!
 - Logistic Regression with constraints (via a NAG optimizer)

Coming Soon
 - Linear Regression with constraints (via a NAG optimizer)
 - Sampled K-means
 - Quantile Analysis
 - Factor Analysis
 - ???

### Why use a commercial library on Spark?
 - Performance
   - Over 100x performace increase from Mllib for linear regression
   - More accurate solutions 
   - Robust optimizers for fewer iterations over data
 - Quality
   - Underneath NAG Spark sits a compiled library 
 - Extensive Documentation and Support
   - Over 1600 tried and tested routines

### The NAG Library
Produced by experts for use in a variety of applications, the NAG Library is the largest commercially available collection of numerical and statistical algorithms in the world. With over 1,600 tried and tested routines that are both flexible and portable it remains at the core of thousands of programs and applications spanning the globe.

NAG Functionality includes
 - Optimization (Linear, Quadratic, Nonlinear, and Least-Squares Solvers)
 - Correlation and Regression Analysis
 - Statistics
 - Ordinary and Partial Differential Equations
 - Curve and Surface Fitting
 - Random Number Generation

### Using the NAG Library on Spark

The NAG Library requires all data must be in-memory when calling NAG functions. As this contrasts with Spark's out-of-memory computing, the NAG Library must be called one of two ways:

1. Break the problem up and call NAG functions on smaller datasets. You can either process all the data or a subset and then aggregate the results together in a final reduce stage (see the NAG Correlation Example).
2. Reformulate the problem using a different method such as a Maximum Likelihood problem. Though this is oftentimes inefficient, this can be the only way to analize big data (see the NAGLogisticRegression Example).

### About NAG
The Numerical Algorithms Group (NAG) is dedicated to applying its expertise in numerical engineering, to delivering high-quality computational software and high performance computing services. For four decades NAG experts have worked closely with world-leading researchers in academia and industry to create powerful, reliable and flexible software which today is relied on by tens of thousands of individual users, as well as numerous independent software vendors. As a not for profit company, NAG reinvests any surpluses on the research and development of its products and services, its staff and fostering new numerical and scientific talent.

[the NAG Library]: http://nag.com/numeric/numerical_libraries.asp
