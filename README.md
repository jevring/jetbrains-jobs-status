# jetbrains-jobs-status

This is a plugin for (all?) jetbrains products that tracks jobs, such as tests runs and project builds/compilations,
and makes them available via a REST endpoint. The idea is that you can build dashboards, or use AI to poll for test
completion etc.

The endpoint is available at [http://127.0.0.1:63342/api/jobs]. If that port is occupied, try 63343, etc. This is
where intellij idea exposes it endpoints. This plugin uses the application-wide rest service for this, so it's all
managed by intellij idea.

To get just builds, use [http://127.0.0.1:63342/api/jobs/builds]. For just tests, use [http://127.0.0.1:63342/api/jobs/tests].
Finally, to get just a single build, use [http://127.0.0.1:63342/api/jobs/builds/$id], where `$id` is a number. See the previous
links for those numbers. They just get incremented for each build/test. You can also get a single test in the same way.

Tests and builds are automatically cleaned up. Once they are no longer active (i.e. after they completed either successfully or
with a failure), they live for a few minutes more before being deleted. The most recent build/test is always kept, no matter how old.

You can pin this port with `-Drpc.port=<port>`, in which case it probably doesn't hunt for ports. I don't know what happens if this port is taken.

![Build](https://github.com/jevring/jetbrains-jobs-status/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/35252.svg)](https://plugins.jetbrains.com/plugin/35252)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/35252.svg)](https://plugins.jetbrains.com/plugin/35252)

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "jetbrains-jobs-status"</kbd> >
  <kbd>Install</kbd>

- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/35252) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is
  running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/35252/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/jevring/jetbrains-jobs-status/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


