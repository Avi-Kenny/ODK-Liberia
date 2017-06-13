package org.lastmilehealth.collect.android.summary;

import android.content.Context;
import android.database.Cursor;

import org.lastmilehealth.collect.android.application.Collect;
import org.lastmilehealth.collect.android.cases.impl.BaseLoadingTask;
import org.lastmilehealth.collect.android.manager.Manager;
import org.lastmilehealth.collect.android.parser.InstanceElement;
import org.lastmilehealth.collect.android.parser.XmlInstanceParser;
import org.lastmilehealth.collect.android.parser.XmlSummaryParser;
import org.lastmilehealth.collect.android.provider.InstanceProviderAPI;
import org.lastmilehealth.collect.android.utilities.FormsUtils;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by Anton Donchev on 13.06.2017.
 */

public class DefaultSummaryLoadingTask extends BaseLoadingTask {


    @Override
    public void run() {
        if (canceled) {
            onEvent(SummaryManager.Event.FAILED);
            return;
        }

        onEvent(SummaryManager.Event.LOADING);

        try {
            XmlSummaryParser parser = new XmlSummaryParser();
            SummaryCollection summaries = parser.parse();
            if (summaries != null) {
                Collection<InstanceElement> elements = new ArrayList<>();
                final Context context = Collect.getInstance();
                Cursor cursor = FormsUtils.getInstancesCursor(context, null);
                XmlInstanceParser instanceParser;
                for (boolean hasElement = cursor.moveToFirst(); hasElement; hasElement = cursor.moveToNext()) {

                    if (canceled) {
                        onEvent(SummaryManager.Event.FAILED);
                        return;
                    }
                    try {
                        String instancePath = cursor.getString(cursor.getColumnIndex(InstanceProviderAPI.InstanceColumns.INSTANCE_FILE_PATH));

                        instanceParser = new XmlInstanceParser(instancePath);
                        InstanceElement element = instanceParser.parse();
                        elements.add(element);
                    } catch (Exception e) {
                        // Continue to next instance.
                    }
                }
                Manager.getSummaryManager().dispose();
                Manager.getSummaryManager().getSummaries().addAll(summaries);
                Manager.getSummaryManager().getInstances().addAll(elements);
                summaries.clear();
                elements.clear();
                onEvent(SummaryManager.Event.LOADED);
            } else {
                onEvent(SummaryManager.Event.FAILED);
            }

        } catch (Exception e) {
            onEvent(SummaryManager.Event.FAILED);
        }

    }

    private void onEvent(final int event) {
        Collect.MAIN_THREAD_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                Manager.getSummaryManager().onEvent(event);
            }
        });
    }

}
