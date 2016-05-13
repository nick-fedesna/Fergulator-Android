package nesdroid

import (
	"encoding/binary"
	"golang.org/x/mobile/exp/f32"
)

type vid struct {
	pixelBuffer chan []uint32
}

var video vid

func GetPixelBuffer() []byte {
	if video.pixelBuffer != nil {
		frame := <-video.pixelBuffer

		buf := make([]float32, len(frame))
		for k, v := range frame {
			buf[k] = float32(v)
		}

		return f32.Bytes(binary.LittleEndian, buf...)
	}
	return nil
}
