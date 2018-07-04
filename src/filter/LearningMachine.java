package filter;

public interface LearningMachine {
	
	void learn(int cls, double[] data);
    //評価
    int trial(double[] data);

}
