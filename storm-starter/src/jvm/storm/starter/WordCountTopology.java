package storm.starter;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.topology.base.BaseBasicBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import storm.starter.spout.RandomSentenceSpout;

import java.util.HashMap;
import java.util.Map;

/**
 * This topology demonstrates Storm's stream groupings and multilang capabilities.
 */
public class WordCountTopology {
	public static void main(String[] args) throws Exception {

		TopologyBuilder builder = new TopologyBuilder();

		builder.setSpout("spout", new RandomSentenceSpout(), 5);

		builder.setBolt("split", new SplitSentence(), 8).shuffleGrouping("spout");
		builder.setBolt("count", new WordCount(), 12).fieldsGrouping("split", new Fields("word"));
		// builder.setBolt("count", new WordCount(), 12).shuffleGrouping("split");

		Config conf = new Config();
		conf.setDebug(false);
		conf.setMessageTimeoutSecs(5000);

		if (args != null && args.length > 0) {
			conf.setNumWorkers(3);

			StormSubmitter.submitTopology(args[0], conf, builder.createTopology());
		} else {
			conf.setMaxTaskParallelism(3);

			LocalCluster cluster = new LocalCluster();
			cluster.submitTopology("word-count", conf, builder.createTopology());

			Thread.sleep(10000);

			cluster.shutdown();
		}
	}

	public static class SplitSentence extends BaseBasicBolt {

		public void execute(Tuple input, BasicOutputCollector collector) {
			String sentence = input.getString(0);
			String[] words = sentence.split(" ");
			for (String s : words) {
				collector.emit(new Values(s));
			}
		}

		public void declareOutputFields(OutputFieldsDeclarer declarer) {
			declarer.declare(new Fields("word"));
		}
	}

	public static class WordCount extends BaseBasicBolt {
		Map<String, Integer> counts = new HashMap<String, Integer>();

		@Override
		public void execute(Tuple tuple, BasicOutputCollector collector) {
			String word = tuple.getString(0);
			Integer count = counts.get(word);
			if (count == null)
				count = 0;
			count++;
			counts.put(word, count);
			collector.emit(new Values(word, count));
		}

		@Override
		public void declareOutputFields(OutputFieldsDeclarer declarer) {
			declarer.declare(new Fields("word", "count"));
		}
	}
}
