#!/usr/bin/env bash
# Regenerates the six test files used by the app.
# Needs ffmpeg with libx264, libx265, libvpx and libopus (check: ffmpeg -encoders).
# Each sample sets its duration once (D) so the video and audio inputs always match.
set -euo pipefail

OUT="$(cd "$(dirname "$0")/.." && pwd)/app/src/main/assets/samples"
mkdir -p "$OUT"
cd "$OUT"

ff() { ffmpeg -y -hide_banner -loglevel error "$@"; }

# 1. Baseline: H.264 + AAC, 1280x720, 5 s
D=5
ff -f lavfi -i testsrc=size=1280x720:rate=30:duration=$D -f lavfi -i sine=frequency=440:duration=$D \
   -c:v libx264 -pix_fmt yuv420p -c:a aac h264_aac_720p.mp4

# 2. No audio track: H.264 only, 640x480, 15 s
D=15
ff -f lavfi -i testsrc=size=640x480:rate=25:duration=$D \
   -c:v libx264 -pix_fmt yuv420p -an h264_noaudio_480p.mp4

# 3. Second codec, same container: HEVC + AAC, 1920x1080, 100 s (shows minutes: 1:40)
#    hvc1 = the MP4 tag Android and Apple players expect for HEVC
D=100
ff -f lavfi -i testsrc=size=1920x1080:rate=30:duration=$D -f lavfi -i sine=frequency=660:duration=$D \
   -c:v libx265 -x265-params log-level=error -tag:v hvc1 -pix_fmt yuv420p -c:a aac hevc_aac_1080p.mp4

# 4. Different container and codecs: VP9 + Opus in WebM, 640x360, 6 s
D=6
ff -f lavfi -i testsrc=size=640x360:rate=30:duration=$D -f lavfi -i sine=frequency=880:duration=$D \
   -c:v libvpx-vp9 -b:v 300k -c:a libopus vp9_opus_360p.webm

# 5. Valid media without video: AAC only, 4 s
D=4
ff -f lavfi -i sine=frequency=440:duration=$D -c:a aac audio_only.m4a

# 6. Not media at all, behind a video extension
echo "this is not a video" > not_a_video.mp4

ls -lh "$OUT"

