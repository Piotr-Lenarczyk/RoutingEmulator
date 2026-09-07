● Comprehensive Code Review

Executive Summary

The codebase has a workable domain model and good test-oriented intent, but the current architecture is heavily coupled
around a few large, stateful classes:

    • NetworkTopologyController acts as a GUI controller, application service, configuration file manager, topology editor, and visual renderer.
    • Router combines router state, configuration editing, validation, mode management, route presentation, and ping orchestration.
    • ForwardingEngine combines packet traversal, topology lookup, route resolution, destination resolution, and return-path verification.
    • CLI commands combine parsing, mutable command state, domain execution, output formatting, and exception translation.
    • Configuration staging is implemented with exposed mutable snapshots and a manual commit merge.
    • Exception types are inconsistent and frequently replaced by generic exceptions or message-based dispatch.

The most important architectural risks are:

    1. Hidden global state through CLIContext.
    2. Mutable state captured during RouterCommand.matches().
    3. Non-transactional configuration commit.
    4. The size and responsibility density of ForwardingEngine and Router.
    5. Exception information being lost through generic wrapping and message inspection.

No files were modified.

──────────────────────────────────────────────────────────────────────

1. Separation of Concerns

1.1 NetworkTopologyController is a GUI/application/domain "God Object"

Location: gui/NetworkTopologyController.java

The controller directly handles:

    • JavaFX controls and event handlers.
    • Creation and deletion of Router, Host, Switch, and Connection objects.
    • Topology mutations.
    • Device/interface discovery.
    • Node positioning and rendering.
    • Configuration file reading and format detection.
    • Configuration parser and generator selection.
    • User-facing exception handling.
    • Opening router CLI and host configuration dialogs.

For example, device creation and visual placement happen in the same methods. Connection creation also adds the domain
Connection, creates its JavaFX Line, updates the canvas, and stores the visual mapping.

Why this is a problem

The controller has many independent reasons to change:

    • A JavaFX layout change.
    • A topology validation change.
    • A configuration format change.
    • A device factory change.
    • A new device type.
    • A change to connection rendering.
    • A change to error presentation.

This makes the class difficult to test without JavaFX and means domain changes propagate directly into presentation
code.

Refactoring proposal

Introduce an application façade, for example:

    • TopologyApplicationService
    • RouterConfigurationService
    • TopologyQueryService
    • TopologyViewModel

The GUI should issue application-level operations such as "add router", "connect interfaces", or "load configuration".
The application service should perform domain operations and return immutable view models or DTOs.

The controller should primarily:

    1. Read UI input.
    2. Call an application service.
    3. Update JavaFX state from the returned result.
    4. Display presentation-specific errors.

This is an Application Service plus MVVM/Presenter structure.

────────────────────────────────────────────

1.2 GUI directly mutates core domain objects

Locations:

    • gui/HostConfigDialog.java
    • gui/NetworkTopologyController.java
    • gui/SimpleCLIDialog.java

HostConfigDialog directly constructs IPAddress, SubnetMask, InterfaceAddress, and HostInterface objects, then mutates
the Host:

    • host.setHostInterface(...)
    • hi.setInterfaceAddress(...)
    • hi.setDefaultGateway(...)

It also invokes ping behavior directly through the domain object.

Why this is a problem

The UI knows:

    • How host configuration is represented.
    • How addresses and subnet masks are constructed.
    • Which fields are mutable.
    • How ping is executed.
    • Which exceptions may be thrown.

This makes the GUI dependent on the internal representation of the domain model rather than on use cases.

Refactoring proposal

Create an application-level host service:

    • HostConfigurationService
    • PingApplicationService

The dialog should pass raw user input or a command object to the service. The service should:

    • Parse and validate the input.
    • Apply a complete configuration atomically.
    • Return a success result or a structured domain error.

This also prevents partially applying an IP address before a gateway fails validation.

──────────────────────────────────────────────────────────────────────────────────────

1.3 GUI depends directly on CLI infrastructure and thread-local state

Location: gui/SimpleCLIDialog.java

The dialog creates a RouterCLIParser and uses CLIContext:

    • CLIContext.setWriter(...)
    • CLIContext.setNetworkTopology(...)
    • CLIContext.clear()

The router CLI commands then retrieve those values globally.

Why this is a problem

The CLI has an implicit precondition: callers must initialize the correct thread-local context before command execution.

This creates several risks:

    • Commands are difficult to invoke in isolation.
    • Commands depend on hidden state rather than method parameters.
    • Multiple CLI sessions on the same thread can interfere with each other.
    • Standalone CLI execution sets a writer in RouterCLI, but does not visibly set a NetworkTopology; ping therefore depends on another caller to initialize it.
    • The GUI is coupled to implementation details of the CLI rather than to a reusable command-execution API.

CLIContext is effectively a Service Locator implemented with ThreadLocal.

Refactoring proposal

Introduce an explicit CLI session abstraction:

    • CliSession
    • CommandExecutionContext
    • CommandOutput
    • CommandExecutor

The context should contain explicit dependencies such as:

    • Router
    • NetworkTopology
    • Writer or output sink

The GUI and terminal CLI should each construct their own session adapter. Commands should receive the context explicitly
or return a result that the adapter renders.

The JLine terminal should also be moved out of RouterCLIParser. The parser should not create a system terminal merely
because a command parser is needed by a JavaFX dialog.

A cleaner division is:

    • CommandParser: parses input.
    • CommandExecutor: executes parsed commands.
    • RouterCLI: JLine adapter.
    • SimpleCLIDialog: JavaFX adapter.

───────────────────────────────────────────────────────

1.4 Configuration file I/O is mixed into the controller

Location: gui/NetworkTopologyController.java, configuration loading and saving methods

The GUI directly:

    • Opens files.
    • Reads file contents.
    • Detects configuration format.
    • Obtains a parser from ConfigurationFactory.
    • Applies configuration to a Router.
    • Selects a generator.
    • Converts parsing exceptions into JavaFX alerts.

Why this is a problem

File selection is a presentation concern. Format detection, parsing, validation, and applying a configuration are
application/domain concerns.

The controller is currently coupled to:

    • Every configuration parser implementation.
    • Every configuration generator implementation.
    • Parser-specific exception types.
    • File format selection rules.

Refactoring proposal

Introduce ports and adapters:

    • ConfigurationLoader
    • ConfigurationWriter
    • ConfigurationApplicationService

The GUI should deal with Path or file chooser results. The service should handle:

    • Format detection.
    • Parsing.
    • Validation.
    • Transactional application.
    • Generation.

This is a natural Ports and Adapters / Hexagonal Architecture boundary.

────────────────────────────────────────────────────

1.5 Visual state uses mutable domain objects as keys

Location: NetworkTopologyController.java

The controller maintains:

    • Map<Object, DeviceNode> deviceNodes
    • Map<Connection, Line> connectionLines

The use of Object requires repeated instanceof checks for Router, Host, and Switch.

Why this is a problem

The visualization is coupled to:

    • Concrete device classes.
    • Their equality and hash-code behavior.
    • Their object identity and lifecycle.
    • The internal representation of connections.

The Object map also prevents the compiler from enforcing device-related invariants.

Refactoring proposal

Introduce a common domain abstraction such as:

    • Device
    • DeviceId
    • DeviceSnapshot

Then use stable IDs for visual mappings:

    • Map<DeviceId, DeviceNode>
    • Map<ConnectionId, ConnectionLine>

The GUI should render immutable topology snapshots instead of retaining domain entities directly.

NetworkTopology should expose domain queries such as findDeviceByInterface (...) rather than requiring the GUI to
iterate through each device type.

───────────────────────────────────────────────────────────────────────────────────────────────

1.6 Core package dependencies are also blurred

Although the domain does not directly import JavaFX, the core package boundaries are not clean:

    • common.NetworkTopology imports Router, Host, and Switch.
    • common.Connection imports RouterInterface and a router exception.
    • Host directly creates PingService.
    • Router directly creates PingService and uses NetworkTopology.

This means common is not a genuinely independent low-level module. It is an aggregate layer that knows about all device
implementations.

Refactoring proposal

Separate:

    • Core network abstractions: Device, NetworkInterface, Connection, addresses.
    • Router domain: routing tables and router behavior.
    • Topology/application layer: relationships between routers, hosts, switches, and connections.
    • Simulation services: forwarding and ping.

The topology aggregate can depend on device abstractions or use a dedicated domain registry rather than importing every
concrete device type.

─────────────────────────────────────────────────

2. Command Pattern and Configuration Staging

2.1 RouterCommand.matches () mutates command state

Locations:

    • router/cli/RouterCommand.java
    • router/cli/route/SetRouteNextHopCommand.java
    • router/cli/PingCommand.java
    • Other route and interface commands

The interface separates matches (String) and execute (Router), but concrete commands parse and store parameters during
matching.

For example, SetRouteNextHopCommand.matches () writes into:

    • destinationSubnet
    • nextHop

execute () then relies on those fields having been populated by a previous call to matches ().

Why this is an anti-pattern

The predicate matches () is not actually a predicate; it has side effects.

Consequences include:

    • Commands are stateful and not safely reusable.
    • Commands are not thread-safe.
    • Direct calls to execute() can operate on stale or null parameters.
    • Parsing and execution are coupled to a specific lifecycle.
    • Tests must reproduce the parser's matching sequence.
    • A command instance cannot naturally represent multiple invocations.

Refactoring proposal

Replace matches () with a parse operation that returns an immutable invocation:

    • Optional<ParsedCommand>
    • CommandInvocation
    • Parameter records such as RouteCommandParameters

The flow should be:

    1. Parse input.
    2. Return an immutable command invocation containing parameters.
    3. Execute the invocation.

The command handler itself can be stateless and reusable.

This is a Command pattern with immutable invocation objects, rather than a mutable parser/command hybrid.

─────────────────────────────────────────────────────────

2.2 Parser registration is hard-coded and order-dependent

Location: router/cli/RouterCLIParser.java

registerCommands () directly instantiates every concrete command and relies on ordering:

    │ "more specific patterns must be registered before general ones"

The parser then performs a sequential search over the list.

Problems

    • Adding a command requires editing the parser.
    • Registration order becomes part of correctness.
    • The parser knows every concrete command implementation.
    • Matching is O(n) and becomes harder to reason about as commands grow.
    • The command registry cannot easily be replaced for tests or alternate CLI modes.

Refactoring proposal

Extract a CommandRegistry or inject a list of command providers into the parser.

A stronger design is:

    • Parse the command verb and structural path first.
    • Dispatch by command path.
    • Use a handler registry keyed by command path.
    • Detect ambiguity explicitly rather than relying on list ordering.

For example, route commands could be registered by operation and target type rather than by regex order.

────────────────────────────────────────────────────

2.3 Prefix matching is a second, inconsistent parser

Location: router/cli/CommandMatcher.java

The parser first attempts exact regex matching and then invokes a separate prefix-matching algorithm based on
getCommandPattern () strings.

Problems

The two mechanisms use different definitions of command syntax:

    • Exact matching uses regular expressions.
    • Prefix matching uses help-text tokens.
    • Prefix matching can only partially understand placeholders.
    • Ambiguity depends on the registered command list.
    • getCommandPattern() is both documentation and executable dispatch metadata.

This makes command behavior difficult to predict and test.

Refactoring proposal

Represent command syntax once using a structured command specification. The same specification should support:

    • Full parsing.
    • Prefix completion.
    • Help display.
    • Ambiguity detection.

Alternatively, use a dedicated grammar/parser layer and make completion consume the same grammar.

──────────────────────────────────────────────

2.4 Many command classes are nearly identical

There are multiple route command families for:

    • Set.
    • Delete.
    • Disable.
    • Next-hop target.
    • Interface target.
    • Optional administrative distance.

These classes repeat:

    • Regex creation.
    • Parameter storage.
    • Output handling.
    • Exception translation.
    • Route construction.

Refactoring proposal

Use a parameterized route mutation command with explicit strategies:

    • RouteOperation: set, delete, disable.
    • RouteTarget: next-hop or interface.
    • Optional distance.
    • Immutable route parameters.

The implementation can delegate route construction and mutation to separate strategies instead of using one large
conditional command.

This preserves the Command pattern while removing mechanical duplication.

─────────────────────────────────────────────────────────────────────────

2.5 Commands combine execution, output, and error translation

Concrete commands commonly:

    1. Obtain PrintWriter through CLIContext.
    2. Execute a router operation.
    3. Catch RuntimeException.
    4. Call CLIErrorHandler.
    5. Print CLI-specific output.

The parser also catches RuntimeException and prints the message.

Problems

This creates multiple error-handling layers:

    • Domain methods throw exceptions.
    • Commands sometimes translate them.
    • The parser catches all runtime exceptions.
    • CLI error formatting depends on the command's configuration path.
    • Output such as [edit] is hard-coded inside command implementations.

Refactoring proposal

Make execution return a result:

    • CommandResult
    • CommandSuccess
    • CommandFailure
    • CommandOutput

The domain should throw domain exceptions. A single CLI adapter should translate those exceptions into CLI messages.
Commands should not need to know whether they are executed by JavaFX, JLine, a test, or another UI.

Mode checks can also be centralized with a command decorator or execution middleware instead of being repeated in each
command.

───────────────────────────────────────────────────────────────────────────────────────────────────

2.6 RouterConfigurationSession is a mutable Unit of Work, but without strong transaction boundaries

Location: router/RouterConfigurationSession.java

The session stores:

    • A staged routing table.
    • Staged interfaces.
    • A mutable hasUncommittedChanges flag.

commitChanges (Router) then:

    1. Iterates through staged interfaces.
    2. Copies fields individually into the committed router.
    3. Adds missing interfaces.
    4. Rebuilds the routing table using RoutingTableCopier.
    5. Clears the dirty flag.

Problems

Manual merge is fragile

Every interface property is copied individually:

    • Address.
    • MAC address.
    • Description.
    • VRF.
    • MTU.
    • Status.

Adding a new property to RouterInterface requires remembering to update the commit logic.

Commit is not explicitly atomic

The committed router is modified before the routing table replacement completes. If a later operation fails, the router
may contain a partially applied configuration.

The session depends on an externally supplied router

Methods such as commitChanges (Router router) and discardChanges (Router router) require the caller to pass the correct
router. This creates an unnecessary association risk.

Mutable staged state is exposed

getStagedRoutingTable () and getStagedInterfaces () expose mutable objects. External code can change staged
configuration without updating hasUncommittedChanges.

The Lombok-generated setters also weaken invariants.

Lifecycle is duplicated

Staged state is discarded in several locations:

    • Session construction.
    • Entering configuration mode.
    • Forced exit.
    • Router reset.
    • Parser cleanup.

The configuration parsers also manually control mode, clear staging, commit, discard, and restore the original mode.

Refactoring proposal

Use a stronger Unit of Work / Memento design:

    1. Create an immutable RouterConfiguration snapshot.
    2. Create a ConfigurationEditor that records changes privately.
    3. Validate the complete candidate configuration.
    4. Build a new immutable committed configuration.
    5. Replace the router configuration in one operation.
    6. Keep discard as restoration of the original snapshot.

The session should own or be bound to exactly one router configuration context. Do not expose mutable staged
collections; expose read-only views.

RouterConfigurationSession should provide operations such as:

    • setInterfaceAddress(...)
    • addRoute(...)
    • removeRoute(...)
    • disableRoute(...)
    • commit()
    • discard()

Rather than allowing callers to mutate the tables directly.

clearStagedConfiguration () should be renamed or split into an explicit operation such as resetCandidateConfiguration
(), because it is not merely a data-structure clear; it creates a deliberate "remove all configuration" candidate.

──────────────────────────────────────

3. SOLID Analysis

3.1 ForwardingEngine violates SRP

Location: common/ForwardingEngine.java

The class is approximately 500 lines and handles all of the following:

    • Host-originated forwarding.
    • Router-originated forwarding.
    • TTL normalization and mutation.
    • Gateway resolution.
    • Main route traversal.
    • Static route interpretation.
    • Interface-route resolution.
    • Next-hop resolution.
    • Host and router-interface destination resolution.
    • Topology lookup.
    • Connection and neighbor lookup.
    • Return-path verification.
    • A second return-path traversal algorithm.
    • Forwarding status messages and logging.

These are separate responsibilities with different reasons to change.

Refactoring proposal

Split the class into collaborators:

    • PacketForwarder — main traversal algorithm.
    • GatewayResolver — host-to-router gateway resolution.
    • RouteResolver — selects and interprets routes.
    • DestinationResolver — resolves hosts, router interfaces, and neighbors.
    • ReturnPathVerifier — checks reverse reachability.
    • TopologyQuery — provides interface/router/connection lookups.
    • ForwardingReason — typed outcome reasons.

ForwardingEngine can remain as a thin façade that coordinates these services.

─────────────────────────────────────────────────────────────────────────────

3.2 Main forwarding and return forwarding duplicate algorithms

Locations:

    • ForwardingEngine.traverse(...)
    • ForwardingEngine.forwardFromRouter(...)
    • resolveDirectSubnet(...)
    • resolveReturnRouteDirectSubnet(...)
    • resolveInterfaceRoute(...)
    • resolveReturnRouteInterfaceRoute(...)

The return-path logic duplicates significant portions of the primary forwarding logic.

Problems

Duplicated algorithms can diverge. For example, the primary interface-route path checks whether an exit interface is
disabled, while the return-route interface path does not apply exactly the same checks.

The verifyOwnAddressReturn boolean also makes the primary traversal operate in two subtly different modes.

Refactoring proposal

Use one traversal engine with an explicit immutable forwarding context:

    • Source.
    • Destination.
    • Hop count.
    • TTL policy.
    • Whether return verification is being performed.

Alternatively, keep return verification separate but make it call a shared low-level traversal service rather than
maintaining a second route-resolution implementation.

Avoid boolean flags whose meaning changes the algorithm substantially. Use a strategy or explicit mode object instead.

────────────────────────────────────────────────────────────────────────

3.3 ForwardingEngine violates DIP through concrete topology dependencies

ForwardingEngine depends directly on:

    • NetworkTopology.
    • Router.
    • RouterInterface.
    • Connection.
    • RouteSelector.

It performs repeated concrete scans in:

    • findRouterOwningInterface(...)
    • findInterfaceByIp(...)

Problems

    • Hard to unit test with small fake topologies.
    • Alternative topology implementations cannot be supplied.
    • Lookup logic is not reusable.
    • Repeated O(n×m) scans are embedded in the forwarding algorithm.
    • Forwarding behavior is coupled to the concrete static-routing model.

Refactoring proposal

Define an abstraction such as TopologyView or TopologyQuery with operations including:

    • Find router owning an interface.
    • Find interface by IP.
    • Find connection for an interface.
    • Find a host reachable through an interface.
    • Determine whether two interfaces are direct neighbors.

Provide a production adapter backed by NetworkTopology and test doubles for unit tests.

A TopologyIndex can maintain interface-to-router and IP-to-interface indexes, avoiding repeated scans.

────────────────────────────────────────────────────────────────────

3.4 Forwarding outcomes mix domain results with presentation strings

ForwardingOutcome is populated with strings such as:

    • "No route"
    • "TTL expired"
    • "Reached host"
    • "Unsupported neighbor type"

Why this is a problem

String reasons are:

    • Easy to mistype.
    • Difficult to compare reliably.
    • Hard to localize or format differently.
    • Coupled to CLI/UI wording.
    • Unable to carry structured context such as the affected interface or router.

Refactoring proposal

Use a typed reason:

    • ForwardingReason.NO_ROUTE
    • ForwardingReason.TTL_EXPIRED
    • ForwardingReason.INTERFACE_ADMIN_DOWN

Store structured fields separately where necessary. CLI and GUI formatters can then choose their own presentation.

────────────────────────────

3.5 Router violates SRP

Location: router/Router.java

Router currently combines:
• Router identity and state. • Committed routing table. • Interface collection. • Operational/configuration mode. •
Configuration staging. • Route validation. • Route mutation. • Interface mutation. • Commit/discard behavior. • Reset
behavior. • Ping service creation. • Routing-table presentation. • User-facing error message construction. • Logging.

Examples include:

    • addRoute, removeRoute, and disableRoute performing mode checks, validation, staging, and logging.
    • setMode enforcing transaction rules and performing discard operations.
    • showIpRoute delegating to a presentation formatter.
    • ping creating a concrete PingService.
    • toString throwing an exception when the router is in configuration mode.

Refactoring proposal

Separate responsibilities into:

    • Router — operational router state and forwarding-relevant domain behavior.
    • RouterConfigurationService — configuration mutations and validation.
    • RouterModeController or a State pattern — mode transitions.
    • ConfigurationSession — candidate configuration lifecycle.
    • RouterPingPort — ping use case.
    • RoutingTablePresenter — CLI/UI formatting.

The router can expose meaningful domain operations, but configuration transaction and presentation concerns should not
be embedded in the same entity.

───────────────────────────────────────────────────

3.6 Router violates DIP through hard-coded services

Router.ping (...) performs:

    • new PingService()

PingService itself owns a concrete:

    • new ForwardingEngine()

This prevents injecting test doubles or alternative forwarding implementations.

Refactoring proposal

Inject abstractions through constructors:

    • PingService
    • ForwardingPort
    • RouteSelectionPolicy
    • TopologyView

A practical migration would be:

    1. Add constructors accepting the dependencies.
    2. Keep a convenience constructor for production defaults.
    3. Move default object construction to a composition root such as Main, an application factory, or a service configuration class.

──────────────────────────────────────────────────────────────────────────────────────────────────────

3.7 Public mutable accessors weaken domain invariants

Lombok @Getter and @Setter are used on Router and NetworkTopology. This exposes mutable references to:

    • Interfaces.
    • Routing tables.
    • Device lists.
    • Connections.
    • Router mode.

Consumers can bypass:

    • Mode validation.
    • Configuration staging.
    • Connection validation.
    • Dirty-state tracking.
    • Link-state updates.

Refactoring proposal

Replace setters with intention-revealing methods:

    • addInterface(...)
    • replaceConfiguration(...)
    • addRouter(...)
    • removeConnection(...)
    • enterConfigurationMode()
    • commitConfiguration()

Return immutable or unmodifiable views from collection getters.

─────────────────────────────────────────────────────

4. Error Handling and Exception Hierarchy

4.1 The exception hierarchy is split and inconsistent

router.exceptions.RouterException is the intended router base class, but:

    • InterfaceAdministrativelyDownException extends RuntimeException directly.
    • InterfaceUnavailableException extends RuntimeException directly.
    • All exceptions in common.exceptions extend RuntimeException directly.
    • ConfigurationParseException also extends RuntimeException independently.

Refactoring proposal

Introduce a shared base for domain failures, for example:

    • NetworkDomainException
       • RouterException
       • TopologyException
       • ConfigurationException

Or use separate, semantically meaningful roots if the modules must remain independent.

InterfaceAdministrativelyDownException belongs conceptually to the network/topology domain, not specifically to the
router package, because it is thrown from common.Connection.

───────────────────────────────────────────────

4.2 common.Connection throws a router exception

Location: common/Connection.java

Connection imports and throws:

    • router.exceptions.InterfaceAdministrativelyDownException

This reverses the expected dependency direction: a common topology primitive depends on a router-specific exception
package.

Refactoring proposal

Move the exception to a common network package, or introduce an interface-level state abstraction such as:

    • InterfaceStateException
    • AdministrativeStateException

Connection should depend only on a common network exception type.

────────────────────────────────────────────────────────────

4.3 Connection destroys exception type and cause information

Connection catches every RuntimeException and wraps it in:

    • IllegalStateException("Could not establish connection " + e.getMessage())

The original exception is not passed as a cause.

Consequences

    • The caller cannot catch InterfaceAdministrativelyDownException.
    • The exception hierarchy becomes ineffective.
    • Stack-trace information is lost.
    • Error handling must inspect generic messages.

Refactoring proposal

Do not catch exceptions that should propagate. If contextual wrapping is required, preserve the type information where
possible and always preserve the cause.

The same rule applies to configuration parsers, which frequently wrap runtime exceptions inside
ConfigurationParseException without a cause.

──────────────────────────────────────────────────

4.4 Exception types are too broad for their usages

DuplicateConfigurationException is used for:

    • Duplicate routes.
    • Duplicate interface addresses.
    • Already-disabled routes.
    • Already-disabled interfaces.

ConfigurationNotFoundException is used for:

    • Missing routes.
    • Missing interface addresses.
    • Enabling an already-enabled interface.

These are not the same semantic conditions.

Refactoring proposal

Use specific exception types or structured error codes:

    • RouteAlreadyExistsException
    • InterfaceAddressAlreadyConfiguredException
    • RouteAlreadyDisabledException
    • InterfaceAlreadyDisabledException
    • RouteNotFoundException
    • InterfaceAddressNotFoundException
    • InterfaceAlreadyEnabledException

Avoid over-fragmenting if unnecessary; a structured ConfigurationErrorCode is also valid. The important point is that
callers should not need to infer the cause from the message.

──────────────────────────────────────────

4.5 CLIErrorHandler relies on message text

Location: router/cli/CLIErrorHandler.java

Examples include checks such as:

    • "Route not found".equals(message)
    • "Route is already disabled".equals(message)
    • message.contains("Cannot assign network address")
    • message.endsWith("is not a valid IPv4 prefix")

Problems

This is brittle because changing a domain message changes program behavior.

There is also a semantic error: InvalidNextHopException and InvalidSubnetException are handled together as invalid IPv4
prefix errors, even though a next-hop error and a destination subnet error should normally produce different messages.

Refactoring proposal

Use exception types and structured fields:

    • Exception type identifies the category.
    • Fields identify the route, interface, address, or command path.
    • CLIErrorHandler maps typed domain errors to CLI output.

Messages should be presentation data, not control-flow keys.

────────────────────────────────────────────────────────────

4.6 Exception constructors are incomplete

Most custom exceptions only accept a message:

    • No cause constructor.
    • No structured context.
    • InterfaceUnavailableException has no useful constructor at all.

Refactoring proposal

Provide consistent constructors:

    • Message-only.
    • Message plus cause.
    • Where appropriate, structured fields or error codes.

Add documentation describing when each exception is thrown.

───────────────────────────────────────────────────────────

4.7 InterfaceUnavailableException appears to be dead code

InterfaceUnavailableException is declared but has no meaningful constructor and no usage in the source tree.

Refactoring proposal

Either remove it or define and implement a clear use case. Unused exception types create uncertainty about intended
failure semantics.

There is also a naming inconsistency: InterfaceAlreadyConnected does not follow the *Exception naming convention used by
the other exception classes.

───────────────────────────────────────────────────────────────────

Recommended Refactoring Sequence

Phase 1: Stabilize boundaries

    1. Replace CLIContext with an explicit command execution context.
    2. Separate JLine terminal creation from RouterCLIParser.
    3. Stop exposing mutable staged tables and topology collections.
    4. Preserve exception causes when wrapping.
    5. Move interface-state exceptions out of the router-specific package.

Phase 2: Improve command and configuration design

    1. Replace mutable matches() state with immutable parsed invocations.
    2. Introduce a command registry and shared command specification.
    3. Consolidate route command variants.
    4. Turn configuration staging into an explicit Unit of Work.
    5. Validate a complete candidate configuration before atomic commit.

Phase 3: Decompose core services

    1. Extract RouterConfigurationService.
    2. Extract RouterModeController.
    3. Inject PingService and ForwardingEngine.
    4. Split ForwardingEngine into traversal, topology lookup, destination resolution, and return-path verification services.
    5. Move route table formatting out of Router.

Phase 4: Introduce a GUI application layer

    1. Add topology and configuration application services.
    2. Make JavaFX controllers consume DTOs or view models.
    3. Move file I/O and configuration format selection out of controllers.
    4. Use stable device/connection IDs for visualization maps.
    5. Extract terminal behavior from SimpleTerminalTextArea into a testable terminal model.

Overall Assessment

The code is functional but currently organized around direct object mutation and procedural orchestration. The strongest
improvement would be to establish explicit boundaries:

JavaFX GUI → Application services → Domain model/services → Infrastructure adapters

At present, the GUI skips the application layer, the CLI uses global context, the router owns too many services, and
forwarding contains several independent algorithms. Introducing explicit services, immutable command/configuration
snapshots, typed outcomes, and dependency injection would substantially improve testability, maintainability, and
extensibility without requiring a complete rewrite.