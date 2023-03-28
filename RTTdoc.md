# Real-Time Transcription

Real-Time Transcription takes the audio content of a host's media stream and transcribes it into written words in real time.
This page shows you how to start and stop Real-Time Transcription in your app, through a business server, then
display the text in your app.

## Understand the tech

To start transcribing the audio in a channel in real-time, you send an `HTTP` request to the Agora SD-RTN™
through your business server. Real-Time Transcription provides the following modes:

* Transcribe speech in real-time, then stream this data to the channel.
* Transcribe speech in real-time, store the text in the `WebVTT` format, and upload the file to third-party cloud
storage.

Real-Time Transcription transcribes at most three speakers in a channel. When there are more than three speakers, the top three are selected based on volume, and their audio is transcribed.

The following figure shows the workflow to start, query, and stop a Real-Time Transcription task:

![Real-Time Transcription business server](https://docs-git-milestone37-speech-to-text-Agora-gdxe.vercel.app/en/assets/images/real-time-transcription-server-07d073102bebc8cf0f41e6985efc56b1.svg)

In order to use the RESTful API to transcribe speech, make the following calls:

1. `acquire`: Request a `builderToken` that authenticates the user and gives permission to start Real-Time Transcription . You must call
`start` using this `builderToken` within five minutes.
1. `start`: Begin the transcription task. Once you start a task, `builderToken` remains valid for the entire
session. Use the same `builderToken` to query and stop the task.
1. `query`: Check the task status.
1. `stop`: Stop the transcription task.

## Prerequisites

In order to set up Real-Time Transcription in your app, you must have:

* Implemented [Get Started with Video Calling](https://docs.agora.io/en/video-calling/get-started/get-started-sdk)
* Enabled Real-Time Transcription for your project. Contact sales@Agora.io
* Activated a [supported cloud storage service](#supported-third-party-cloud-storage-services) to record and store Real-Time Transcription videos and texts
* Installed the [Protobuf package](https://protobuf.dev/downloads) to generate code classes for displaying transcription text.
* To run the post-processing script:
  * Python 3.0
  * [`ffmpeg`](https://ffmpeg.org/download.html) and `ffplay`

## Implement a business server

You create a business server as a bridge between your app and Agora Real-Time Transcription.
Implementing a business server to manage Real-Time Transcription provides the following benefits:

* Improved security as your `apiKey`, `apiSecret`, `builderToken`, and `taskId`, are not exposed to the client.
* Token processing is securely handled on the business server.
* Avoid splicing complex request body strings on the client side to reduce the probability of errors.
* Implement additional functionality on the business server. For example, billing for  Real-Time Transcription use, checking
user privileges and payment status of a user.
* If the REST API is updated, you do not need to update the client.

To import the API collection for testing and to obtain sample code for your business server, see the [Postman Collection](https://documenter.getpostman.com/view/6319646/SVSLr9AM#69bd200a-7543-4104-8ccc-415741abbeb7). 

## Use Google Protobuffer Generator to parse text data

Google Protocol buffers are an extensible and language-neutral mechanism for serializing transcription data.
Protobuffer enables you to generate source code in multiple languages, based on a specified structure. For more information about Google protocol buffers, see [protobuf.dev](https://protobuf.dev/).

Agora provides the following protobuffer template for parsing Real-Time Transcription data:

```
syntax = "proto3";

package Agora.audio2text;
option java_package = "io.Agora.rtc.audio2text";
option java_outer_classname = "Audio2TextProtobuffer";

message Text {
  int32 vendor = 1;
  int32 version = 2;
  int32 seqnum = 3;
  int32 uid = 4;
  int32 flag = 5;
  int64 time = 6;
  int32 lang = 7;
  int32 starttime = 8;
  int32 offtime = 9;
  repeated Word words = 10;
}
message Word {
  string text = 1;
  int32 start_ms = 2;
  int32 duration_ms = 3;
  bool is_final = 4;
  double confidence = 5;
}
```

To read and display the Real-Time Transcription text in your client:

1. Copy the protobuffer template to a local file.

1. In your local file, edit the following properties to match your project:
  
   - `package`: The source code package namespace.  
   - `option`: The language for which you want to generate the class. For example, Java or Javascript.

2. [Generate a Protobuffer class](https://protobuf.dev/programming-guides/proto3/#generating).

    You invoke the `protoc` protocol compiler on your local file.

Agora also provides Protobuf sample code to parse and display transcription text. To obtain the sample code, contact sales@Agora.io

## Synchronize transcription files with the cloud recording

The `m3u8+vtt` file generated by Real-Time Transcription, and the `m3u8+ts` file generated by [Cloud Recording](../../../cloud-recording/overview/product-overview.mdx) are two independent files. The time stamp references in these media
files are different, and not synchronized. The cloud recording time stamp starts at `0`, while the `m3u8+vtt` uses the system time stamp. If either process starts abnormally, the media files generated by the two services may be out of sync during playback.

Post-processing ensures synchronization of subtitles and recorded audio. It enables you to associate the `m3u8+ts` file generated by cloud recording with the `m3u8+vtt` file generated by Real-Time Transcription.

Agora provides a post-processing script that enables you to synchronize the two files.

#### Run the post-processing script

To synchronize files generated by Real-Time Transcription, take the following steps:

1. Unzip the [post-processing script](https://github.com/AgoraIO/Docs-Source/files/10931258/add_webvtt.zip) to a local folder.

1. Run the script on your Real-Time Transcription files:

    ```shell
     python3 insert_subtitle.py --av audio_dir/audio_ts.m3u8 --subtitle subtitle_dir/subtitle.m3u8 --output output_dir/ --overwrite
     ```

     If `ffmpeg/ffprob` are not in your `PATH`, use`–ffmpeg_path` to specify the path.

1. Play the synchronized files:

    1. Start the HTTP server by running the following command:

       ```shell
       python3 -m http.server --bind 127.0.0.1 -doutput_dir
       ```

    1. In your browser, enter the following URL:

       ```
       http://127.0.0.1:8000/player_demo.html
       ```

## Reference

This section contains information that completes the information in this page, or points you to documentation that explains other aspects to this product.

### REST API

Refer to the Real-Time Transcription [REST API documentation](https://docs.agora.io/en/api-reference?platform=rest) for parameter details.

### List of supported languages

Use the following language codes in the `recognizeConfig.language` parameter of the start request. The current version supports at most two languages, separated by commas. 

| Language                         | Code  | 
| -------------------------------- | ----- |
| Chinese (Cantonese, Traditional) | zh-HK |
| Chinese (Mandarin, Simplified)   | zh-CN |
| Chinese (Taiwanese Putonghua)    | zh-TW |
| English (India)                  | en-IN |
| English (US)                     | en-US |
| French (French)                  | fr-FR |
| German (Germany)                 | de-DE |
| Hindi (India)                    | hi-IN |
| Indonesian (Indonesia)           | id-ID |
| Italian (Italy)                  | it-IT |
| Japanese (Japan)                 | ja-JP |
| Korean (South Korea)             | ko-KR |
| Portuguese (Portugal)            | pt-PT |
| Spanish (Spain)                  | es-ES |

### Supported third-party cloud storage services

The following third-party cloud storage service providers are supported:

* [Alibaba Cloud](https://www.alibabacloud.com/product/oss)
* [Amazon S3](https://aws.amazon.com/s3/?nc1=h_ls)
* [Baidu AI Cloud](https://intl.cloud.baidu.com/product/bos.html)
* [Google Cloud](https://cloud.google.com/storage)
* [Huawei Cloud](https://www.huaweicloud.com/intl/en-us/product/obs.html)
* [Kingsoft Cloud](https://en.ksyun.com/nv/product/KS3.html)
* [Microsoft Azure](https://azure.microsoft.com/en-us/services/storage/blobs/)
* [Qiniu Cloud](https://www.qiniu.com/en/products/kodo)
* [Tencent Cloud](https://intl.cloud.tencent.com/product/cos)
