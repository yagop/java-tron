package org.tron.program;

import io.netty.util.internal.PlatformDependent;
import java.lang.reflect.Field;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DirectMemoryReporterImpl {
  private static final int _1K = 1024;
  private static final String BUSINESS_KEY = "netty_direct_memory";
  private AtomicLong directMemory;

  @PostConstruct
  public void init() throws Exception {
    Field field = PlatformDependent.class.getDeclaredField("DIRECT_MEMORY_COUNTER");
    field.setAccessible(true);
    directMemory = ((AtomicLong) field.get(PlatformDependent.class));
    startReport();
  }

  public void startReport() {
    Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::doReport, 0, 1, TimeUnit.SECONDS);
  }

  private void doReport() {
    try {
      int memoryInKb = (int) (directMemory.get()/ _1K);
      logger.info("{}: {}k", BUSINESS_KEY, memoryInKb);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
  }
}
