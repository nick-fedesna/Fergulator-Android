package main

/*
#include <jni.h>
*/
import "C"

import (
	"fmt"
	"github.com/scottferg/Fergulator/nes"
	"log"
	"os"
	"runtime"
	"runtime/pprof"
	"unsafe"
)

var (
	cachePath string
	audioOut  *Audio
)

func main() {
	runtime.GOMAXPROCS(runtime.NumCPU())
	nes.AudioEnabled = true
	defer audioOut.Close()
}

// Use JNI_OnLoad to ensure that the go runtime is initialized at a predictable time,
// namely at System.loadLibrary()
//export JNI_OnLoad
func JNI_OnLoad(vm *C.JavaVM, reserved unsafe.Pointer) C.jint {
	return C.JNI_VERSION_1_6
}

//export Java_com_ferg_afergulator_Engine_setFilePath
func Java_com_ferg_afergulator_Engine_setFilePath(env *C.JNIEnv, clazz C.jclass, path C.jstring) {
	if path != nil {
		cachePath = GetJavaString(env, path)
		log.Printf("FILE PATH: %v\n", cachePath)
	}
}

//export Java_com_ferg_afergulator_Engine_loadRom
func Java_com_ferg_afergulator_Engine_loadRom(env *C.JNIEnv, clazz C.jclass, jbytes C.jbyteArray, name C.jstring) C.jboolean {
	defer func() {
		if err := recover(); err != nil {
			log.Printf("panic: loadRom: %v\n", err)
		}
	}()

	nes.GameName = GetJavaString(env, name)
	rom := GetJavaByteArray(env, jbytes)

	if len(cachePath) > 0 {
		nes.SaveStateFile = fmt.Sprintf("%s/%s.save", cachePath, nes.GameName)
		nes.BatteryRamFile = fmt.Sprintf("%s/%s.save", cachePath, nes.GameName)

		log.Printf("cache path: %s", cachePath)
	}

	log.Printf("%v ROM: %v (%v kb)\n", string(rom[:3]), nes.GameName, len(rom)/1024)

	if audioOut == nil {
		audioOut = NewAudio()
	}
	videoTick, err := nes.Init(rom, audioOut.AppendSample, GetKey)
	if err != nil {
		log.Println(err)
		return C.JNI_FALSE
	}

	video.pixelBuffer = videoTick

	// Main runloop, in a separate goroutine so that
	// the video rendering can happen on this one
	go nes.RunSystem()

	log.Printf("NES SYSTEM RUNNING.......")

	return C.JNI_TRUE
}

//export Java_com_ferg_afergulator_Engine_pauseEmulator
func Java_com_ferg_afergulator_Engine_pauseEmulator(env *C.JNIEnv, clazz C.jclass) {
	defer func() {
		if err := recover(); err != nil {
			log.Printf("panic: init: %v\n", err)
		}
	}()
}

//export Java_com_ferg_afergulator_Engine_enableAudio
func Java_com_ferg_afergulator_Engine_enableAudio(env *C.JNIEnv, clazz C.jclass, enabled C.jboolean) {
	defer func() {
		if err := recover(); err != nil {
			log.Printf("panic: enable sound: %v\n", err)
		}
	}()

	nes.AudioEnabled = enabled == C.JNI_TRUE
}

//export Java_com_ferg_afergulator_Engine_saveBatteryRam
func Java_com_ferg_afergulator_Engine_saveBatteryRam(env *C.JNIEnv, clazz C.jclass) {
	defer func() {
		if err := recover(); err != nil {
			log.Printf("panic: init: %v\n", err)
		}
	}()

    nes.SaveBatteryFile()
}

//export Java_com_ferg_afergulator_Engine_saveState
func Java_com_ferg_afergulator_Engine_saveState(env *C.JNIEnv, clazz C.jclass) {
	defer func() {
		if err := recover(); err != nil {
			log.Printf("panic: init: %v\n", err)
		}
	}()

	nes.SaveGameState()
}

//export Java_com_ferg_afergulator_Engine_loadState
func Java_com_ferg_afergulator_Engine_loadState(env *C.JNIEnv, clazz C.jclass) {
	defer func() {
		if err := recover(); err != nil {
			log.Printf("panic: init: %v\n", err)
		}
	}()

	nes.LoadGameState()
}

//export Java_com_ferg_afergulator_Engine_keyEvent
func Java_com_ferg_afergulator_Engine_keyEvent(env *C.JNIEnv, clazz C.jclass, key C.jint, event C.jint, player C.jint) {
	//	log.Printf("key [%v] %v\n", key, event)
	p := int(player)
	if nes.Pads[player] != nil {
		if event == 1 {
			nes.Pads[player].KeyDown(int(key), p)
		} else {
			nes.Pads[player].KeyUp(int(key), p)
		}
	}
}

func GetKey(ev interface{}) int {
	if k, ok := ev.(int); ok {
		return k
	}

	return -1
}

//export Java_com_ferg_afergulator_Engine_startProfile
func Java_com_ferg_afergulator_Engine_startProfile(env *C.JNIEnv, clazz C.jclass) {
	defer func() {
		if err := recover(); err != nil {
			log.Printf("panic: init: %v\n", err)
		}
	}()

	f, err := os.Create(fmt.Sprintf("%s/profile.prof", cachePath))
	if err != nil {
		log.Println(err.Error())
	}
	pprof.StartCPUProfile(f)
	log.Println("Profile started")
}

//export Java_com_ferg_afergulator_Engine_stopProfile
func Java_com_ferg_afergulator_Engine_stopProfile(env *C.JNIEnv, clazz C.jclass) {
	defer func() {
		if err := recover(); err != nil {
			log.Printf("panic: init: %v\n", err)
		}
	}()

	pprof.StopCPUProfile()
	log.Println("Profile stopped")
}
