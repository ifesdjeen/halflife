package halflife.mesos;

import org.apache.mesos.*;
import org.apache.mesos.Protos.*;

public class HalflifeExecutor implements Executor {

  @Override
  public void registered(ExecutorDriver executorDriver,
                         ExecutorInfo executorInfo,
                         FrameworkInfo frameworkInfo,
                         SlaveInfo slaveInfo) {

  }

  @Override
  public void reregistered(ExecutorDriver executorDriver, SlaveInfo slaveInfo) {

  }

  @Override
  public void disconnected(ExecutorDriver executorDriver) {

  }

  @Override
  public void launchTask(ExecutorDriver executorDriver, TaskInfo taskInfo) {

  }

  @Override
  public void killTask(ExecutorDriver executorDriver, TaskID taskID) {

  }

  @Override
  public void frameworkMessage(ExecutorDriver executorDriver, byte[] bytes) {

  }

  @Override
  public void shutdown(ExecutorDriver executorDriver) {

  }

  @Override
  public void error(ExecutorDriver executorDriver, String s) {

  }
}
