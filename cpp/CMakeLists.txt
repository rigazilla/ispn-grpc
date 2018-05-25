# Minimum CMake required
cmake_minimum_required(VERSION 2.8)

# Project
project(HelloWorld C CXX)

if(NOT MSVC)
  set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")
else()
  add_definitions(-D_WIN32_WINNT=0x600)
endif()

# Protobuf
# NOTE: we cannot use "CONFIG" mode here because protobuf-config.cmake
# is broken when used with CMAKE_INSTALL_PREFIX
find_package(Protobuf REQUIRED)
message(STATUS "Using protobuf ${protobuf_VERSION}")

find_library(GRPC++_UNSECURE_LIBRARY NAME grpc++_unsecure HINTS "/opt/local/lib")
find_library(GRPC_LIBRARY NAME grpc HINTS "/opt/local/lib")
message(STATUS "Using grpc++_unsecure ${GRPC++_UNSECURE_LIBRARY}")
message(STATUS "Using grpc ${GRPC_LIBRARY}")
# {Protobuf,PROTOBUF}_FOUND is defined based on find_package type ("MODULE" vs "CONFIG").
# For "MODULE", the case has also changed between cmake 3.5 and 3.6.
# We use the legacy uppercase version for *_LIBRARIES AND *_INCLUDE_DIRS variables
# as newer cmake versions provide them too for backward compatibility.
if(Protobuf_FOUND OR PROTOBUF_FOUND)
  if(TARGET protobuf::libprotobuf)
    set(_PROTOBUF_LIBPROTOBUF protobuf::libprotobuf)
  else()
    set(_PROTOBUF_LIBPROTOBUF ${PROTOBUF_LIBRARIES})
    include_directories(${PROTOBUF_INCLUDE_DIRS})
  endif()
  if(TARGET protobuf::protoc)
    set(_PROTOBUF_PROTOC $<TARGET_FILE:protobuf::protoc>)
  else()
    set(_PROTOBUF_PROTOC ${PROTOBUF_PROTOC_EXECUTABLE})
  endif()
else()
  message(WARNING "Failed to locate libprotobuf and protoc!")
endif()

# gRPC
message(STATUS "Using gRPC ${gRPC_VERSION}")

# gRPC C++ plugin

# Proto file
get_filename_component(hw_proto "../src/main/proto/cache.proto" ABSOLUTE)
get_filename_component(hw_proto_path "${hw_proto}" PATH)

# Generated sources
protobuf_generate_cpp(hw_proto_srcs hw_proto_hdrs "${hw_proto}")
set(hw_grpc_srcs "${CMAKE_CURRENT_BINARY_DIR}/cache.grpc.pb.cc")
set(hw_grpc_hdrs "${CMAKE_CURRENT_BINARY_DIR}/cache.grpc.pb.h")
add_custom_command(
      OUTPUT "${hw_grpc_srcs}" "${hw_grpc_hdrs}"
      COMMAND ${_PROTOBUF_PROTOC}
      ARGS --grpc_out "${CMAKE_CURRENT_BINARY_DIR}" -I "${hw_proto_path}"
        --plugin=protoc-gen-grpc="/opt/local/bin/grpc_cpp_plugin"
        "${hw_proto}"
      DEPENDS "${hw_proto}")

get_filename_component(hw_proto_types "../src/main/proto/cache-types.proto" ABSOLUTE)
get_filename_component(hw_proto_path_types "${hw_proto_types}" PATH)

# Generated sources
protobuf_generate_cpp(hw_proto_types_srcs hw_proto_types_hdrs "${hw_proto_types}")
set(hw_grpc_types_srcs "${CMAKE_CURRENT_BINARY_DIR}/cache-types.grpc.pb.cc")
set(hw_grpc_types_hdrs "${CMAKE_CURRENT_BINARY_DIR}/cache-types.grpc.pb.h")
add_custom_command(
      OUTPUT "${hw_grpc_types_srcs}" "${hw_grpc_types_hdrs}"
      COMMAND ${_PROTOBUF_PROTOC}
      ARGS --grpc_out "${CMAKE_CURRENT_BINARY_DIR}" -I "${hw_proto_path}"
        --plugin=protoc-gen-grpc="/opt/local/bin/grpc_cpp_plugin"
        "${hw_proto_types}"
      DEPENDS "${hw_proto_types}")

# Generated include directory
include_directories("${CMAKE_CURRENT_BINARY_DIR}")

# Targets 
foreach(_target
  cache_client sample)
  add_executable(${_target} "${_target}.cc"
    ${hw_proto_srcs}
    ${hw_grpc_srcs}
    ${hw_proto_types_srcs}
    ${hw_grpc_types_srcs})
  target_link_libraries(${_target}
    ${_PROTOBUF_LIBPROTOBUF}
    ${GRPC++_UNSECURE_LIBRARY}
    ${GRPC_LIBRARY}
    )
endforeach()
