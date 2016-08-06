package co.thnki.whistleblower.services;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.squareup.otto.Subscribe;

import co.thnki.whistleblower.singletons.Otto;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class InternetJobService extends JobService
{
    static final String JOB_FINISHED = "jobFinished";
    static final String JOB_NOT_FINISHED = "jobNotFinished";
    private JobParameters mJobParameters;

    @Override
    public boolean onStartJob(JobParameters jobParameters)
    {
        Otto.register(this);
        Log.d("LagIssue", "onStartJob : " + jobParameters.getJobId());
        mJobParameters = jobParameters;
        Intent intent = new Intent(this, RemoteConfigService.class);
        Log.d("RemoteConfigService", "RemoteConfigService Started  : InternetJobService");
        startService(intent);
        return true;
    }

    @Subscribe
    public void isJobFinished(String status)
    {
        switch (status)
        {
            case JOB_FINISHED:
                jobFinished(mJobParameters, false);
                break;
            case JOB_NOT_FINISHED:
                jobFinished(mJobParameters, true);
                break;
        }
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters)
    {
        Log.d("LagIssue", "onStopJob : " + jobParameters.getJobId());
        Otto.unregister(this);
        return false;
    }
}
