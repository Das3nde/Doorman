package tabbie.doorman;

import org.acra.*;
import org.acra.annotation.*;

@ReportsCrashes(formKey="dE9zdkV3dVM3M21KRG1EeG53MlpYOHc6MQ")

public class TabbieDoorman extends android.app.Application
{
	@Override
	public void onCreate()
	{
		// The following line triggers the initialization of ACRA
		ACRA.init(this);
		super.onCreate();
	}
}
