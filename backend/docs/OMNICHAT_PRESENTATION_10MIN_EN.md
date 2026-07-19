# OmniChat - 10 Minute English Presentation

## Slide 1 - Title

**OmniChat: Centralized Omnichannel Chat Management**

**Speaker notes:**
Good morning everyone. Today I would like to present OmniChat, a centralized omnichannel chat management system designed for businesses that sell and support customers across multiple social and e-commerce platforms.

In modern social commerce, customers no longer contact a business through just one channel. They may send a message from Facebook Messenger, Zalo Official Account, TikTok Shop, Shopee, or other platforms. For customers, this is convenient. But for businesses and support teams, it creates a serious operational challenge: conversations are spread everywhere.

OmniChat solves this problem by bringing messages from many channels into one unified inbox, helping agents respond faster, managers assign work more fairly, and businesses avoid missing customer opportunities.

## Slide 2 - The Real-World Problem

**Common problems before OmniChat:**

- Messages are scattered across Facebook, Zalo, Shopee, TikTok, and browser tabs.
- Agents must switch between many apps during the day.
- Customer messages can be missed during peak hours.
- Response time becomes slower, especially during campaigns or flash sales.
- Managers cannot easily see who is handling which conversation.
- Customer history is fragmented across different platforms.

**Speaker notes:**
Before a system like OmniChat, many small and medium businesses manage customer messages manually. One staff member may open Facebook Page inbox, another checks Zalo OA, another handles Shopee chat, and someone else watches TikTok Shop.

This workflow looks simple at first, but it becomes very hard to control when message volume increases. During a sale campaign, a business may receive hundreds or thousands of messages in a short time. Some messages are answered twice, some are not answered at all, and some customers wait too long and decide to buy from another shop.

The real problem is not only technical. It is also a management problem. The business cannot clearly know the workload of each agent, the average response time, or which channel brings the most conversations.

## Slide 3 - Business Impact

**Without a centralized system, businesses face:**

- Lost sales opportunities because customers do not receive fast replies.
- Lower customer satisfaction due to inconsistent service.
- Inefficient agent workflow and duplicated work.
- No single source of truth for conversations.
- Weak reporting for agent performance and channel effectiveness.

**Speaker notes:**
In social commerce, response speed directly affects revenue. A customer asking about price, stock, shipping, or promotion usually expects an immediate answer. If the business replies late, the customer may leave.

Another challenge is consistency. If a customer chats on Facebook today and Zalo tomorrow, the support team may not recognize that this is the same person. The conversation history is split, and the agent does not have enough context.

From the manager's perspective, it is also difficult to evaluate performance. How many conversations did each agent handle? Which channel created the highest workload? Which conversations breached the SLA? These questions are hard to answer when data is separated across platforms.

## Slide 4 - OmniChat Solution

**OmniChat provides a unified inbox for multiple channels.**

Core idea:

- Receive messages from external platforms through webhooks.
- Normalize different message formats into one internal event format.
- Store conversations and messages centrally.
- Route new conversations to available agents.
- Allow agents to reply from one interface.
- Send replies back to the original platform.

**Speaker notes:**
OmniChat is designed as the single working place for customer conversations. Instead of asking agents to open many tools, the system receives messages from different platforms and displays them in one inbox.

For example, a customer sends a message from Facebook Messenger. Another customer sends a message from Zalo OA. OmniChat receives both messages, converts them into a common internal format, stores them as conversations, and shows them in the same inbox.

When an agent replies inside OmniChat, the system knows the original channel of the conversation and sends the reply back through the correct platform API, such as Facebook Send API or Zalo OA API.

## Slide 5 - How The System Works

**High-level flow:**

1. Customer sends a message from Facebook, Zalo, or another channel.
2. The platform sends a webhook to OmniChat.
3. API Gateway routes the request to Integration Service.
4. Integration Service validates, deduplicates, and normalizes the message.
5. Kafka publishes the message event.
6. Conversation Service stores the conversation and message.
7. Routing Service assigns the conversation to an available agent.
8. WebSocket Service pushes realtime updates to the agent UI.

**Speaker notes:**
The system uses an event-driven microservices architecture. This design helps OmniChat scale and handle high message volume.

The Integration Service acts as the bridge between outside platforms and the internal system. Every platform has its own payload format, so this service works as an anti-corruption layer. It converts Facebook, Zalo, and future platforms into a standard event.

Kafka is used as the message backbone. It allows services to communicate asynchronously and reliably. The Conversation Service focuses on conversations and messages. The Routing Service focuses on agent assignment and workload. The WebSocket Service focuses on realtime updates to the frontend.

This separation makes the system easier to scale and maintain.

## Slide 6 - Main Feature: Unified Inbox

**Unified Inbox capabilities:**

- Display all conversations in one screen.
- Show channel badges such as Facebook and Zalo.
- Sort conversations by latest activity.
- Filter by status: unassigned, open, or closed.
- Click a conversation to view full chat history.
- Send replies directly from the same screen.

**Speaker notes:**
The most important feature is the unified inbox. This is where agents spend most of their time.

On the inbox page, conversations from multiple channels are listed together. Each conversation shows useful information such as the channel, customer identity, status, assigned agent, and last activity time.

When an agent clicks a conversation, the system displays the full chat history between the customer and the business. Customer messages, agent replies, and system messages are clearly separated. The agent can type a reply and send it without leaving the page.

This reduces context switching and helps agents work faster.

## Slide 7 - Main Feature: Smart Routing

**Routing and assignment:**

- New conversations start as unassigned.
- Agents have statuses: online, busy, or offline.
- The system can assign conversations based on availability and workload.
- Supervisors can manually transfer conversations.
- Workload tracking helps distribute work more fairly.

**Speaker notes:**
A centralized inbox alone is not enough. When many new conversations arrive, the system also needs to decide who should handle them.

OmniChat includes a Routing Service that manages agent availability and workload. If an agent is online and has capacity, the system can assign new conversations automatically. This avoids the situation where one agent receives too many chats while another agent is idle.

The system also supports manual assignment or transfer. For example, if a conversation requires technical knowledge, a supervisor can transfer it to a more suitable agent.

This routing capability improves both speed and fairness.

## Slide 8 - Main Feature: Realtime And Reliability

**Realtime and reliability features:**

- WebSocket updates for new conversations and new messages.
- Redis is used for idempotency and session-related data.
- Duplicate webhook messages are ignored.
- Kafka keeps services loosely coupled.
- Delivery status can be updated after sending to external platforms.

**Speaker notes:**
Realtime experience is very important for a chat system. Agents should not need to refresh the page manually to see new messages. OmniChat uses WebSocket to push updates to the frontend when important conversation events happen.

Reliability is also important because external platforms may retry webhook delivery. Without protection, the same message could be stored twice. OmniChat uses idempotency checks with Redis to detect duplicate message IDs and ignore repeated events.

Kafka also increases reliability by decoupling services. If one service is temporarily slow, other services can continue working, and messages can still be processed through the event pipeline.

## Slide 9 - Key Benefits

**Benefits for each role:**

- Agents: one workspace, faster replies, less switching between apps.
- Supervisors: better visibility, manual transfer, workload control.
- Admins: centralized channel integration and future reporting.
- Customers: faster and more consistent responses.
- Business: fewer missed chats, better conversion, stronger operational control.

**Speaker notes:**
OmniChat creates value for different users.

For agents, it simplifies daily work. They no longer need to monitor many platforms separately. For supervisors, it gives visibility into which conversations are open, which agent is responsible, and when intervention is needed.

For customers, the benefit is simple: faster response and better service. For the business, this means fewer missed opportunities, higher conversion, and better data for decision making.

In the long term, OmniChat can also support analytics, SLA monitoring, quick replies, tagging, and mini CRM features to improve productivity even further.

## Slide 10 - Conclusion

**OmniChat turns scattered messages into one manageable workflow.**

Final message:

- The problem: customer conversations are fragmented across many platforms.
- The solution: a centralized, realtime, omnichannel inbox.
- The approach: event-driven microservices with Integration, Conversation, Routing, Customer, and WebSocket services.
- The value: faster replies, fewer missed chats, fairer workload, and better customer experience.

**Speaker notes:**
To conclude, OmniChat is built to solve a very practical problem in social commerce: too many messages, too many platforms, and not enough centralized control.

By unifying messages from Facebook, Zalo, and future channels into one inbox, OmniChat helps teams respond faster and manage conversations more professionally. Its architecture also supports scalability, reliability, and future expansion.

The final goal is not only to build a chat tool, but to build an operational platform where customer conversations become easier to manage, measure, and improve.

Thank you for listening.
