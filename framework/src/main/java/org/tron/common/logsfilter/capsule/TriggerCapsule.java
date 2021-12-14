package org.tron.common.logsfilter.capsule;

import org.tron.common.logsfilter.trigger.Trigger;

public abstract class TriggerCapsule {

  public abstract void processTrigger();

  public abstract Trigger getTrigger();
}
