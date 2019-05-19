/**
 * Inspired by examples/rdkafka_consumer_example.cpp in
 * librdkafka(https://github.com/edenhill/librdkafka),
 * whose license is the following.
 * modifications in CMakeList.txt:
 * add_executable(KafkaLogWriter KafkaLogWriter.cpp)
 * target_link_libraries(KafkaLogWriter PUBLIC stdc++fs rdkafka++)
 */

/*
 * librdkafka - Apache Kafka C library
 *
 * Copyright (c) 2014, Magnus Edenhill
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */


#include <iostream>
#include <string>
#include <vector>
#include <memory>
#include <chrono>
#include <ctime>
#include <fstream>
#include <unordered_map>

#include <filesystem>
#include <experimental/filesystem>
#include "rdkafkacpp.h"

#include <csignal>

using std::cout;
using std::endl;
using std::ends;
using std::cerr;
using std::vector;
using std::string;
using std::unique_ptr;
using std::unordered_map;
using std::ofstream;
using std::chrono::system_clock;

namespace fs=std::filesystem;

static bool exit_eof = false;

static int verbosity = 1;

static int eof_cnt = 0;

static int partition_cnt = 0;

static bool run = true;

static long msg_cnt = 0;

static int64_t msg_bytes = 0;

static unordered_map<string, ofstream> files{};

static void sigterm(int sig)
{
    run = false;
}

class ExampleEventCb: public RdKafka::EventCb
{
public:
    void event_cb(RdKafka::Event &event)
    {
        time_t tt = system_clock::to_time_t(system_clock::now());
        cout << std::ctime(&tt) << endl;

        switch (event.type())
        {
        case RdKafka::Event::EVENT_ERROR:
            if (event.fatal())
            {
                cerr << "FATAL ";
                run = false;
            }
            cerr << "ERROR (" << RdKafka::err2str(event.err()) << "): " <<
                 event.str() << endl;
            break;

        case RdKafka::Event::EVENT_STATS:std::cerr << "\"STATS\": " << event.str() << endl;
            break;

        case RdKafka::Event::EVENT_LOG:
            fprintf(stderr, "LOG-%i-%s: %s\n",
                    event.severity(), event.fac().c_str(), event.str().c_str());
            break;

        case RdKafka::Event::EVENT_THROTTLE:
            cerr << "THROTTLED: " << event.throttle_time() << "ms by " <<
                 event.broker_name() << " id " << (int) event.broker_id() << endl;
            break;

        default:
            cerr << "EVENT " << event.type() <<
                 " (" << RdKafka::err2str(event.err()) << "): " <<
                 event.str() << endl;
            break;
        }
    }
};

void msg_consume(RdKafka::Message *message, void *opaque)
{
    switch (message->err())
    {
    case RdKafka::ERR__TIMED_OUT:break;

    case RdKafka::ERR_NO_ERROR:
        /* Real message */
        msg_cnt++;
        msg_bytes += message->len();
        if (verbosity >= 3)
            cerr << "Read msg at offset " << message->offset() << endl;
        RdKafka::MessageTimestamp ts;
        ts = message->timestamp();
        if (verbosity >= 2 &&
            ts.type != RdKafka::MessageTimestamp::MSG_TIMESTAMP_NOT_AVAILABLE)
        {
            std::string tsname = "?";
            if (ts.type == RdKafka::MessageTimestamp::MSG_TIMESTAMP_CREATE_TIME)
                tsname = "create time";
            else if (ts.type == RdKafka::MessageTimestamp::MSG_TIMESTAMP_LOG_APPEND_TIME)
                tsname = "log append time";
            cout << "Timestamp: " << tsname << " " << ts.timestamp << endl;
        }
        if (verbosity >= 2 && message->key())
        {
            cout << "Key: " << *message->key() << endl;
        }
        if (verbosity >= 1)
        {
            string content{static_cast<const char *>(message->payload()), static_cast<unsigned long>(message->len())};
//            printf("%.*s\n",
//                   static_cast<int>(message->len()),
//                   static_cast<const char *>(message->payload()));
            cout << content << endl;
            files.at(message->topic_name()) << content << endl;
        }
        break;

    case RdKafka::ERR__PARTITION_EOF:
        /* Last message */
        if (exit_eof && ++eof_cnt == partition_cnt)
        {
            cerr << "%% EOF reached for all " << partition_cnt <<
                 " partition(s)" << endl;
            run = false;
        }
        break;

    case RdKafka::ERR__UNKNOWN_TOPIC:
    case RdKafka::ERR__UNKNOWN_PARTITION:std::cerr << "Consume failed: " << message->errstr() << endl;
        run = false;
        break;

    default:
        /* Errors */
        cerr << "Consume failed: " << message->errstr() << endl;
        run = false;
    }
}

int main()
{
    std::cout << "Hello, World!" << std::endl;

    fs::path path{"./topics"};

    if (!fs::exists(path))
    {
        cerr << absolute(path) << " does not exists." << endl;
        exit(1);
    }
    if (!fs::is_directory(path))
    {
        cerr << absolute(path) << " is not dir." << endl;
        exit(1);
    }

    vector<string> topics{};
    for (auto &p:fs::directory_iterator(path))
    {
        if (p.is_regular_file())
        {
            auto name = p.path().filename().string();
            cout << "load " << name << endl;
            topics.push_back(name);
        }
    }
    cout << "topic count = " << topics.size() << endl;


    unique_ptr<RdKafka::Conf> conf{RdKafka::Conf::create(RdKafka::Conf::CONF_GLOBAL)};
    unique_ptr<RdKafka::Conf> tconf{RdKafka::Conf::create(RdKafka::Conf::CONF_TOPIC)};

    string errstr;
    conf->set("enable.partition.eof", "true", errstr);

    if (conf->set("group.id", "klw", errstr) != RdKafka::Conf::CONF_OK)
    {
        cerr << errstr << endl;
        exit(1);
    }

    string brokers = "localhost:9092";
    cout << argc << endl;
    if (argc == 3 && strcmp(argv[1], "-b") == 0)
    {
        brokers = string{argv[2]};
    }
    cout << "brokers -> " << brokers << endl;
    conf->set("metadata.broker.list", brokers, errstr);

    ExampleEventCb ex_event_cb;
    conf->set("event_cb", &ex_event_cb, errstr);

    conf->set("default_topic_conf", tconf.get(), errstr);
    tconf.reset(nullptr);

    signal(SIGINT, sigterm);
    signal(SIGTERM, sigterm);

    // prepare FILE
    // Because of SmallStringOptimization, I don't think pass by ref/value matters.
    for (auto &name:topics)
    {
        auto one = files.try_emplace(name, name,std::ios_base::app);
        if (one.second)
        {
            auto &os = one.first->second;
            if (os.fail())
            {
                cerr << "Failed to open file: " << name << endl;
                exit(1);
            }
            time_t tt = system_clock::to_time_t(system_clock::now());
            os << "\nappend since " << std::ctime(&tt) << endl;
        }
    }

    // create consumer
    unique_ptr<RdKafka::KafkaConsumer> consumer{RdKafka::KafkaConsumer::create(conf.get(), errstr)};
    if (!consumer)
    {
        cerr << "Failed to create consumer: " << errstr << endl;
        exit(1);
    }
    conf.reset(nullptr);
    cout << "% Created consumer " << consumer->name() << endl;

    // subscribe topics
    RdKafka::ErrorCode err = consumer->subscribe(topics);
    if (err)
    {
        cerr << "Failed to subscribe to " << topics.size() << " topics: "
             << RdKafka::err2str(err) << endl;
        exit(1);
    }

    // consume message
    while (run)
    {
        unique_ptr<RdKafka::Message> msg{consumer->consume(1000)};
        msg_consume(msg.get(), nullptr);
        msg.reset(nullptr);
    }

    // stop consumer
    consumer->close();
    consumer.reset(nullptr);
    std::cerr << "% Consumed " << msg_cnt << " messages ("
              << msg_bytes << " bytes)" << std::endl;

    // close FILE
    for (auto &pair :files)
    {
        pair.second.close();
    }

    RdKafka::wait_destroyed(5000);

    return 0;
}