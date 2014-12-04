/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal;

import org.apache.ignite.*;
import org.apache.ignite.compute.*;
import org.apache.ignite.lang.*;
import org.gridgain.grid.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * This test checks that GridTaskListener is only called once per task.
 */
@SuppressWarnings("deprecation")
@GridCommonTest(group = "Kernal Self")
public class GridTaskListenerSelfTest extends GridCommonAbstractTest {
    /** */
    public GridTaskListenerSelfTest() {
        super(/*start grid*/true);
    }

    /**
     * Checks that GridTaskListener is only called once per task.
     *
     * @throws Exception If failed.
     */
    @SuppressWarnings({"BusyWait", "unchecked"})
    public void testGridTaskListener() throws Exception {
        final AtomicInteger cnt = new AtomicInteger(0);

        IgniteInClosure<IgniteFuture<?>> lsnr = new CI1<IgniteFuture<?>>() {
            @Override public void apply(IgniteFuture<?> fut) {
                assert fut != null;

                cnt.incrementAndGet();
            }
        };

        Ignite ignite = G.grid(getTestGridName());

        assert ignite != null;

        ignite.compute().localDeployTask(TestTask.class, TestTask.class.getClassLoader());

        ComputeTaskFuture<?> fut = executeAsync(ignite.compute(), TestTask.class.getName(), null);

        fut.listenAsync(lsnr);

        fut.get();

        while (cnt.get() == 0) {
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException e) {
                error("Got interrupted while sleep.", e);

                break;
            }
        }

        assert cnt.get() == 1 : "Unexpected GridTaskListener apply count [count=" + cnt.get() + ", expected=1]";
    }

    /** Test task. */
    private static class TestTask extends ComputeTaskSplitAdapter<Serializable, Object> {
        /** {@inheritDoc} */
        @Override protected Collection<? extends ComputeJob> split(int gridSize, Serializable arg) throws GridException {
            Collection<ComputeJobAdapter> jobs = new ArrayList<>();

            for (int i = 0; i < 5; i++) {
                jobs.add(new ComputeJobAdapter() {
                    @Override public Serializable execute() {
                        return 1;
                    }
                });
            }

            return jobs;
        }

        /** {@inheritDoc} */
        @Override public Object reduce(List<ComputeJobResult> results) {
            return null;
        }
    }
}
