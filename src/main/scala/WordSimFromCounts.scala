import org.apache.spark.SparkContext
import org.apache.spark.SparkConf

object WordSimFromCounts {
    def main(args: Array[String]) {
        if (args.size < 1) {
            println("Usage: WordSim word-feature-counts word-counts feature-counts output [w=1000] [s=0.0] [t=2] [sig=LMI] [p=1000] [l=200]")
            println("For example, the arguments \"wikipedia wikipedia-out 500 0.0 3\" will override w with 500 and t with 3, leaving the rest at the default values")
            return
        }

        val wordFeatureCountsFile = args(0)
        val wordCountsFile = args(1)
        val featureCountsFile = args(2)
        val outDir = args(3)
        val param_w = if (args.size > 4) args(4).toInt else 1000
        val param_s = if (args.size > 5) args(5).toDouble else 0.0
        val param_t = if (args.size > 6) args(6).toInt else 2
        val param_sig = if (args.size > 7) args(7) else "LMI"
        val param_p = if (args.size > 8) args(8).toInt else 1000
        val param_l = if (args.size > 9) args(9).toInt else 200

        def sig(_n:Long, wc:Long, fc:Long, bc:Long) = if (param_sig == "LMI") WordSimUtil.lmi(_n,wc,fc,bc) else WordSimUtil.ll(_n,wc,fc,bc)

        val conf = new SparkConf().setAppName("WordSim")
        val sc = new SparkContext(conf)

        val wordFeatureCounts = sc.textFile(wordFeatureCountsFile)
            .map(line => line.split("\t"))
            .map({case Array(word, feature, count) => (word, (feature, count.toInt))})

        val wordCounts = sc.textFile(wordCountsFile)
            .map(line => line.split("\t"))
            .map({case Array(word, count) => (word, count.toInt)})

        val featureCounts = sc.textFile(featureCountsFile)
            .map(line => line.split("\t"))
            .map({case Array(feature, count) => (feature, count.toInt)})

        val wordSimsWithFeatures = WordSimUtil.computeWordSimsWithFeatures(wordFeatureCounts, wordCounts, featureCounts,
            param_w, param_t, param_s, param_p, param_l, sig, outDir)

        wordSimsWithFeatures
            .map({case (word1, (word2, score, featureSet)) => word1 + "\t" + word2 + "\t" + score + "\t" + featureSet.mkString("  ")})
            .saveAsTextFile(outDir + "__SimWithFeatures")
    }
}