import 'dart:io';
import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutterx_intent/action.dart' as android_action;
import 'package:flutterx_intent/flutterx_intent.dart' as android_intent;

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  late MyAppDataModel _myAppDataModel;

  @override
  void initState() {
    _myAppDataModel = MyAppDataModel();
    _myAppDataModel.inputClickState.add([]);
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text(
            'Plugin Example App',
            style: TextStyle(
              color: Colors.black,
            ),
          ),
          centerTitle: true,
          backgroundColor: Colors.cyanAccent,
        ),
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              StreamBuilder<List<String>>(
                initialData: const [],
                stream: _myAppDataModel.outputResult,
                builder: (context, snapshot) => Padding(
                  padding: const EdgeInsets.only(
                    left: 8,
                    right: 8,
                    top: 12,
                    bottom: 24,
                  ),
                  child: snapshot.hasData
                      ? snapshot.data!.isNotEmpty
                      ? ClipRRect(
                    borderRadius: BorderRadius.circular(24),
                    child: Image.file(
                      File(snapshot.data![0]),
                      fit: BoxFit.cover,
                      width: MediaQuery.of(context).size.width * .75,
                      height:
                      MediaQuery.of(context).size.height * .35,
                    ),
                  )
                      : const Center()
                      : const CircularProgressIndicator(),
                ),
              ),
              TextButton(
                onPressed: () => android_intent.FlutterIntent()
                  ..setAction(android_action.Action.ACTION_IMAGE_CAPTURE)
                  ..startActivityForResult().then(
                        (data) => print(data),
                    onError: (e) => print(e.toString()),
                  ),
                child: const Text(
                  'Intent',
                  style: TextStyle(color: Colors.black),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class MyAppDataModel {
  final StreamController<List<String>> _streamController =
  StreamController<List<String>>.broadcast();

  Sink<List<String>> get inputClickState => _streamController;

  Stream<List<String>> get outputResult =>
      _streamController.stream.map((data) => data);

  dispose() => _streamController.close();
}
