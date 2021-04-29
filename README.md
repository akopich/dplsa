# dplsa
Here we implement the Robust PLSA [1] using Spark. Classical PLSA [2] is implemented as well. Both versions support user-defined regularization. 

# Usage

First, prepare the data
```scala
// tokenize the data
val rawDocuments = sc.parallelize(Seq("a b a", "x y y z", "a b z x ").map(_.split(" ").toSeq))

// enumerate the tokens
val tokenIndexer = new TokenEnumerator().setRareTokenThreshold(0)

// use token indexer to generate tokenIndex
val tokenIndex = tokenIndexer(rawDocuments)

//broadcast token index
val tokenIndexBC = sc.broadcast(tokenIndex)

// replace the tokens with the respective indices 
val docs = rawDocuments.map(tokenIndexBC.value.transform)
```

Second, set up the learner

```scala
val numberOfTopics = 2
val numberOfIterations = 10

val plsa = new RobustPLSA(sc,
    numberOfTopics,
    numberOfIterations,
    new Random(13),
    new SymmetricDirichletDocumentOverTopicDistributionRegularizer(0.2f),
    new SymmetricDirichletTopicRegularizer(0.2f))
```

Finally, do the inference
```scala
val (docParameters, global) = plsa.infer(docs)
```

But say, we need a distribution of unobserved documents over the topics. Then, having prepared `foldInDocs` in the same way as we prepared `docs`, we code
```scala
val foldedInDocParameters = plsa.foldIn(foldInDocs, global)
```

For the full examples, refer the [tests](https://github.com/akopich/dplsa/tree/master/src/test/scala/topicmodeling).

[1] Potapenko A., Vorontsov K. (2013) Robust PLSA Performs Better Than LDA. 

[2] Hofmann, T. (1999) Probabilistic Latent Semantic Analysis.
