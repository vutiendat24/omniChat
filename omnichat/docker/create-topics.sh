#!/bin/bash
# Create Kafka topics for OmniChat (run from host, targets Docker container)
echo "Waiting for Kafka to be ready..."
sleep 5

# Default configuration for Local (RF=1)
RF=1
PARTITIONS=6

TOPICS=(
  "omnichat.conversation.events:$PARTITIONS:$RF"
  "omnichat.customer.events:$PARTITIONS:$RF"
  "omnichat.integration.events:$PARTITIONS:$RF"
  "omnichat.websocket.events:$PARTITIONS:$RF"
  "omnichat.notification.events:3:$RF"
)

for topic_config in "${TOPICS[@]}"; do
  TOPIC_NAME=$(echo $topic_config | cut -d: -f1)
  PARTS=$(echo $topic_config | cut -d: -f2)
  REP_FACTOR=$(echo $topic_config | cut -d: -f3)
  
  echo "Creating topic: $TOPIC_NAME"
  docker exec omnichat-kafka /opt/kafka/bin/kafka-topics.sh --create --if-not-exists \
    --bootstrap-server localhost:9092 \
    --topic $TOPIC_NAME \
    --partitions $PARTS \
    --replication-factor $REP_FACTOR
done

# Config log compaction for customer topic
echo "Configuring log compaction for omnichat.customer.events..."
docker exec omnichat-kafka /opt/kafka/bin/kafka-configs.sh --alter \
  --bootstrap-server localhost:9092 \
  --entity-type topics \
  --entity-name omnichat.customer.events \
  --add-config cleanup.policy=compact
  
echo "All topics created successfully!"
