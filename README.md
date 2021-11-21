# GuardianEyes

## object detection
- stair case dataset in YOLO format: https://akshayk07.weebly.com/real-time-stair-detection.html
- stair_preprocess.py는 YOLO format dataset을 TFLite Model Maker에서 사용할 수 있는 형태인 stair.csv로 바꿔줍니다.
- Model Maker Object Detection Tutorial Stair case.ipynb 는 TFLite 샐러드 감지 예제코드를 조금 수정한 것입니다. 
- 참고 링크:  
https://codelabs.developers.google.com/tflite-object-detection-android#8  
https://www.youtube.com/watch?v=sarZ_FZfDxs

## Midterm presentation link
https://docs.google.com/presentation/d/1MAk7BvqR4HZWOYEZu85BdtR31GxwC12R_6se4th3iEM/edit#slide=id.pv



3d sound feed back sdk 함수들입니다. 


            //사운드 파일은 다음을 호출하여 메모리에 미리 로드할 수 있습니다.
            void gvr_audio_update(gvr_audio_context* api);
            bool gvr_audio_preload_soundfile(gvr_audio_context* api,
                                 const char* filename);

            //사용하지 않은 사운드 파일은 다음을 호출하여 언로드할 수 있습니다.
            void gvr_audio_unload_soundfile(gvr_audio_context* api,
                                const char* filename);



            gvr_audio_source_id
            gvr_audio_create_sound_object(gvr_audio_context* api,
                              const char* filename);



            //이것은 다음 두 함수에 대한 호출을 통해 사운드 객체의 위치 및 볼륨과 같은 속성을 설정하는 데 사용할 수 있는 핸들을 반환합니다.
            void
            gvr_audio_set_sound_object_position(gvr_audio_context* api,
                    gvr_audio_source_id sound_object_id,
            float x, float y, float z);

            void
            gvr_audio_set_sound_volume(gvr_audio_context* api,
                    gvr_audio_source_id source_id, float volume);

            //리스너와의 거리에 따른 Sound Object의 동작은 다음 메서드를 호출하여 제어할 수 있습니다.
            void gvr_audio_set_sound_object_distance_rolloff_model(
                    gvr_audio_context* api, gvr_audio_source_id sound_object_id,
                    int32_t rolloff_model, float min_distance, float max_distance);
