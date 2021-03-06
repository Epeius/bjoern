package octopus.server.commands.executeplugin;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.JSONObject;

import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.orient.server.config.OServerCommandConfiguration;
import com.orientechnologies.orient.server.config.OServerEntryConfiguration;
import com.orientechnologies.orient.server.network.protocol.http.OHttpRequest;
import com.orientechnologies.orient.server.network.protocol.http
		.OHttpRequestException;
import com.orientechnologies.orient.server.network.protocol.http.OHttpResponse;
import com.orientechnologies.orient.server.network.protocol.http.OHttpUtils;
import com.orientechnologies.orient.server.network.protocol.http.command
		.OServerCommandAbstract;

import octopus.server.components.pluginInterface.IPlugin;
import octopus.server.components.pluginInterface.PluginLoader;

public class ExecutePluginCommand extends OServerCommandAbstract
{
	String pluginName;
	String pluginDir;
	String pluginClass;
	JSONObject settings;

	public ExecutePluginCommand(
			final OServerCommandConfiguration iConfiguration)
	{
		readConfiguration(iConfiguration);
	}

	private void readConfiguration(OServerCommandConfiguration iConfiguration)
	{
		for (OServerEntryConfiguration param : iConfiguration.parameters)
		{
			switch (param.name)
			{
				case "dir":
					pluginDir = param.value;
					break;
			}
		}
	}

	@Override
	public boolean execute(OHttpRequest iRequest,
			OHttpResponse iResponse) throws Exception
	{
		OLogManager.instance().warn(this, "startplugin");

		parseContent(iRequest.content);

		long startTime = System.nanoTime();
		executePlugin();
		long stopTime = System.nanoTime();
		double elapsedTime = (stopTime - startTime) / (1000000.0 * 1000);

		iResponse.send(OHttpUtils.STATUS_OK_CODE, "OK", null,
				String.format("Execution time: %.3fs\n", elapsedTime), null);
		return false;
	}

	private void parseContent(String content)
	{
		if(content == null)
			throw new RuntimeException("Error: no content");

		JSONObject data = new JSONObject(content);
		pluginName = data.getString("plugin");
		pluginClass = data.getString("class");
		settings = data.getJSONObject("settings");
	}

	private void executePlugin()
	{
		Path path = Paths.get(pluginDir, pluginName);
		IPlugin plugin = PluginLoader.load(path, pluginClass);

		if (plugin == null)
		{
			throw new OHttpRequestException(
					"Error while loading plugin " + pluginName);
		}
		try
		{
			plugin.configure(settings);
			plugin.beforeExecution();
			plugin.execute();
			plugin.afterExecution();
		} catch (Exception e)
		{
			throw new OHttpRequestException(e.getMessage());
		}
	}

	@Override
	public String[] getNames()
	{
		return new String[]{"POST|executeplugin/"};
	}
}
