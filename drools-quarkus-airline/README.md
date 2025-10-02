# drools-quarkus-airline

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

This is a simple demo on how to use Drools and Quarkus to create a simple MCP server that handles the Airline Chatbot use case to provide deterministic decisions for refunds to customers. 

#Here is why you would want to use a Rules engine in combination with a Chatbot for this purpose:
Using a deterministic rules engine to control airline refunds with a chatbot offers several benefits:
- *Consistency*: Ensures consistent application of refund policies, reducing errors and disputes.
- *Accuracy*: Automates refund calculations based on predefined rules, minimizing manual errors.
- *Efficiency*: Streamlines the refund process, reducing processing time and increasing customer satisfaction.
- *Transparency*: Provides clear explanations for refund decisions, enhancing customer trust and understanding.
- *Compliance*: Ensures adherence to regulatory requirements and airline policies, reducing the risk of non-compliance.

A deterministic rules engine can be particularly useful in scenarios like:
- *Refund eligibility*: Determining whether a passenger is eligible for a refund based on fare type, ticket conditions, and travel dates.
- *Refund amount calculation*: Calculating the refund amount based on the ticket price, fees, and taxes.
- *Refund processing*: Automating the refund process, including generating refund documents and notifying passengers.

By integrating a deterministic rules engine with a chatbot, airlines can provide a seamless and efficient refund experience for passengers, while also reducing operational costs and improving compliance.

# Why use an MCP Server and what is it?

Model Context Protocol (MCP) is a protocol designed to manage and provide context to large language models (LLMs) and other AI systems. It enables these models to access external knowledge sources, retrieve relevant information, and incorporate it into their responses.

*Key aspects of MCP:*

- *Context retrieval*: MCP allows LLMs to retrieve relevant context from external sources, such as databases, knowledge graphs, or documents.
- *Context management*: MCP manages the context provided to LLMs, ensuring that the models have access to the most relevant and up-to-date information.
- *Improved accuracy*: By providing LLMs with relevant context, MCP can improve the accuracy and relevance of their responses.

*Applications of MCP:*

- *Question answering*: MCP can be used to improve the accuracy of question answering systems by providing relevant context to LLMs.
- *Conversational AI*: MCP can be used to enhance conversational AI systems by providing context-aware responses.
- *Knowledge retrieval*: MCP can be used to retrieve relevant information from external sources, improving the knowledge and accuracy of LLMs.

MCP has the potential to significantly improve the performance and capabilities of LLMs and other AI systems.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
mvn package
```

The application can be installed using:

```shell script
mvn install
```
The application can be run on the command line or by the AI Application using:

```shell script
jbang --quiet org.acme:drools-quarkus-airline:1.0.0-SNAPSHOT:runner
```

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides

- REST Jackson ([guide](https://quarkus.io/guides/rest#json-serialisation)): Jackson serialization support for Quarkus REST. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it, not used at the moment, but may add for the compensation call.

- Drools ([guide](https://quarkus.io/guides/drools)): Define and execute your business rules with Drools

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
